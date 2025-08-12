package com.example.bnk_project_02s.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.bnk_project_02s.entity.Bank;
import com.example.bnk_project_02s.repository.BankRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchImportService {

    private final BankRepository repo;

    // 필요시 @Bean 주입으로 교체 가능
    private final RestTemplate rest = new RestTemplate();

    // 환경변수 VWORLD_API_KEY 로도 대체 가능
    @Value("${vworld.api-key:${VWORLD_API_KEY:}}")
    private String vworldApiKey;

    private static final String VWORLD_URL = "https://api.vworld.kr/req/address";

    /* ===================== ① CSV에서 불러와서 저장 ===================== */

    /** 클래스패스 CSV 실행: resources/bnk_branches_utf8.csv */
    @Transactional
    public int importFromClasspathCsv() throws Exception {
        var resource = new ClassPathResource("bnk_branches_utf8.csv");
        try (var in = resource.getInputStream()) {
            return importFromCsvInputStream(in);
        }
    }

    /** 업로드 스트림으로 실행 (운영자 업로드용) */
    @Transactional
    public int importFromCsvInputStream(InputStream in) throws Exception {
        int processed = 0;

        try (var br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) return 0;
            header = header.replace("\uFEFF", ""); // BOM 제거

            List<String> headerCols = parseCsvLine(header);
            Map<String, Integer> idx = indexByName(headerCols);
            require(idx, "bno"); require(idx, "bname"); require(idx, "baddress");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                List<String> cols = parseCsvLine(line);

                Long   bno      = toLong(get(cols, idx, "bno"));
                String bname    = trim(get(cols, idx, "bname"));
                String bphone   = trim(get(cols, idx, "bphone"));
                String baddress = trim(get(cols, idx, "baddress"));
                String bdigital = Optional.ofNullable(trim(get(cols, idx, "bdigital"))).orElse("N");

                if (bno == null || bname == null || baddress == null) continue;

                // 저장은 정제본으로(원본 보존이 필요하면 별도 컬럼 추가 권장)
                String cleaned = cleanAddress(baddress);

                // 타입 결정(도로명 추정) → VWorld 호출 (ROAD → 실패시 PARCEL)
                String type = looksLikeRoad(cleaned) ? "ROAD" : "PARCEL";
                double[] latlng = geocodeVWorld(cleaned, type);
                if (latlng == null) latlng = geocodeVWorld(cleaned, "PARCEL".equals(type) ? "ROAD" : "PARCEL");

                Bank bank = repo.findById(bno).orElseGet(Bank::new);
                bank.setBno(bno);
                bank.setBname(bname);
                bank.setBphone(bphone);
                bank.setBaddress(cleaned);
                bank.setBdigital(bdigital);

                if (latlng != null) {
                    bank.setBlatitude(String.format(Locale.ROOT, "%.7f", latlng[0])); // 위도
                    bank.setBlongitude(String.format(Locale.ROOT, "%.7f", latlng[1])); // 경도
                } else {
                    log.warn("❌ 지오코딩 실패(신규 CSV): bno={} | q='{}'", bno, cleaned);
                }

                repo.save(bank);
                processed++;

                // 간단 레이트 리밋 (약 6~7 qps)
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            }
        }
        log.info("✅ CSV import done. processed={}", processed);
        return processed;
    }

    /* ============ ② DB에 이미 있는 데이터 중 좌표 NULL만 채우기 ============ */

    /** 좌표가 비어있는 레코드만 지오코딩해서 업데이트 */
    @Transactional
    public int fillMissingCoordsFromDb() {
        var targets = repo.findAllMissingCoords(); // ← 성능상 이 쿼리 사용
        log.info("🔎 targets to geocode = {}", targets.size());

        int updated = 0;
        for (var b : targets) {
            String base = b.getBaddress();
            String query = cleanAddress(base);           // 안전빵으로 한 번 더 정리
            if (query.isBlank()) {
                log.warn("skip empty address: bno={}", b.getBno());
                continue;
            }

            String type = looksLikeRoad(query) ? "ROAD" : "PARCEL";
            double[] latlng = geocodeVWorld(query, type);
            if (latlng == null) latlng = geocodeVWorld(query, "PARCEL".equals(type) ? "ROAD" : "PARCEL");

            if (latlng == null) {
                log.warn("❌ 지오코딩 실패(존재 레코드): bno={} | type={} | q='{}'", b.getBno(), type, query);
                continue;
            }

            b.setBlatitude(String.format(Locale.ROOT, "%.7f", latlng[0]));
            b.setBlongitude(String.format(Locale.ROOT, "%.7f", latlng[1]));
            updated++;

            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }
        log.info("✅ geocode updated rows = {}", updated);
        return updated;
    }

    /* ===================== VWorld 지오코딩 호출 ===================== */

    /** 성공 시 [lat, lng] 반환, 실패 시 null */
    private double[] geocodeVWorld(String address, String typeUpper) {
        if (vworldApiKey == null || vworldApiKey.isBlank()) {
            log.error("VWorld API key is missing");
            return null;
        }
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(VWORLD_URL)
                    .queryParam("service", "address")
                    .queryParam("version", "2.0")
                    .queryParam("request", "getCoord")
                    .queryParam("key", vworldApiKey)
                    .queryParam("format", "json")
                    .queryParam("errorFormat", "json")
                    .queryParam("simple", "true")   // 간략 응답 → point 바로 접근
                    .queryParam("refine", "true")   // 주소 정제
                    .queryParam("crs", "EPSG:4326") // WGS84
                    .queryParam("type", typeUpper)  // "ROAD" or "PARCEL"
                    .queryParam("address", address)
                    .build()                                     // ⚠️ 기본 빌드
                    .encode(StandardCharsets.UTF_8)              // ✅ 반드시 인코딩
                    .toUri();

            ResponseEntity<VWorldResp> resp = rest.getForEntity(uri, VWorldResp.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("VWorld HTTP fail: {} | url={}", 
                        (resp != null ? resp.getStatusCode() : "null"), uri);
                return null;
            }

            VWorldResp.Response r = resp.getBody().getResponse();
            if (r == null || !"OK".equalsIgnoreCase(r.getStatus())) {
                log.warn("VWorld status={} | type={} | q='{}' | url={}", 
                        (r != null ? r.getStatus() : "null"), typeUpper, address, uri);
                return null;
            }
            if (r.getResult() == null || r.getResult().getPoint() == null) {
                log.warn("VWorld no result | type={} | q='{}' | url={}", typeUpper, address, uri);
                return null;
            }

            String x = r.getResult().getPoint().getX(); // 경도
            String y = r.getResult().getPoint().getY(); // 위도
            if (x == null || y == null) return null;

            return new double[]{ Double.parseDouble(y), Double.parseDouble(x) }; // [lat, lng]
        } catch (Exception e) {
            log.error("VWorld error | type={} | q='{}' | {}", typeUpper, address, e.toString());
            return null;
        }
    }

    /* ===================== 주소 전처리/판별 ===================== */

    /** 괄호 내용, '*' 이후 문구 제거 + 공백 정리 */
    private static String cleanAddress(String addr) {
        if (addr == null) return "";
        return addr
                .replaceAll("\\([^)]*\\)", "") // 모든 괄호 제거
                .replaceAll("\\*.*$", "")      // '*' 이후 문구 제거(점심시간 등)
                .replaceAll("\\s+", " ")       // 공백 정리
                .trim();
    }

    /** 도로명 주소 추정: '로'/'길' 포함 + 숫자(건물번호) 있으면 ROAD 성향 */
    private static boolean looksLikeRoad(String a){
        if (a == null) return false;
        boolean hasRoadWord = a.contains("로") || a.contains("길");
        boolean hasNumber   = a.matches(".*\\d+.*");
        return hasRoadWord && hasNumber;
    }

    /* ===================== CSV & 유틸 ===================== */

    private static Map<String, Integer> indexByName(List<String> headerCols) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerCols.size(); i++) {
            String key = headerCols.get(i)
                    .replace("\uFEFF","")
                    .replaceAll("^\"|\"$", "")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            map.put(key, i);
        }
        return map;
    }
    private static void require(Map<String,Integer> idx, String name) {
        if (!idx.containsKey(name)) throw new IllegalArgumentException("CSV header missing: " + name);
    }
    private static String get(List<String> cols, Map<String,Integer> idx, String name) {
        Integer i = idx.get(name);
        if (i == null || i < 0 || i >= cols.size()) return null;
        return cols.get(i);
    }
    private static String trim(String s){ return s == null ? null : s.trim(); }
    private static Long toLong(String s){
        try { return (s == null || s.isBlank()) ? null : Long.parseLong(s.trim()); }
        catch(Exception e){ return null; }
    }
    private static boolean isBlank(String s){ return s == null || s.isBlank(); }

    /** 따옴표/콤마 포함 필드 대응 간단 CSV 파서 */
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString());
        return out;
    }

    /* ===================== VWorld 응답 DTO ===================== */
    @lombok.Data
    public static class VWorldResp {
        private Response response;
        @lombok.Data public static class Response {
            private String status;  // "OK"
            private Result result;
        }
        @lombok.Data public static class Result {
            private Point point;
        }
        @lombok.Data public static class Point {
            private String x; // 경도
            private String y; // 위도
        }
    }
}
