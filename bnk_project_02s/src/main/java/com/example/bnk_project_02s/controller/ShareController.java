package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.auth.HmacUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/share")
@CrossOrigin(origins = "*") // 운영에서는 신뢰 도메인으로 제한 권장
@RequiredArgsConstructor
public class ShareController {

    @Qualifier("urlHmac")
    private final HmacUtil urlHmac;

    @Value("${app.origin}")        // 예: http://10.0.2.2:8093  (뒤에 슬래시 없이)
    private String appOrigin;

    /** 공유용 보안 URL 생성 (text/plain으로 URL만 반환) */
    @GetMapping(value = "/product/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createShareUrl(@PathVariable String id) {
        long exp = Instant.now().plus(Duration.ofMinutes(30)).getEpochSecond(); // 만료 30분
        int v = 1;

        String idEnc = urlEncodePathSeg(id);                  // 경로 세그먼트용 인코딩
        String data  = "id=" + idEnc + "&exp=" + exp + "&v=" + v;  // 발급/검증 동일 포맷
        String sigHex = urlHmac.hmacHex(data);

        // 검증/리디렉트 엔드포인트로 유도
        String shareUrl = String.format("%s/api/share/p/%s?exp=%d&v=%d&sig=%s",
                base(), idEnc, exp, v, sigHex);

        return ResponseEntity.ok(shareUrl);
    }

    /** 공유 URL 랜딩(검증 → 실제 상품 URL로 302 리디렉트) */
    @GetMapping("/p/{id}")
    public void openShared(@PathVariable String id,
                           @RequestParam long exp,
                           @RequestParam int v,
                           @RequestParam String sig,
                           HttpServletResponse res) throws IOException {
        // 1) 만료 체크
        long now = Instant.now().getEpochSecond();
        if (exp < now) {
            res.sendError(410, "Expired link");
            return;
        }

        // 2) 서명 재계산 (발급 시와 완전히 동일한 정규화/포맷)
        String idEnc = urlEncodePathSeg(id);
        String data  = "id=" + idEnc + "&exp=" + exp + "&v=" + v;
        String expectedHex = urlHmac.hmacHex(data);

        if (!constantTimeEqualsHex(expectedHex, sig)) {
            res.sendError(403, "Invalid signature");
            return;
        }

        // 3) 목적지 URL 조회(예: DB 조회로 교체)
        String target = resolveProductTargetUrl(id);
        if (target == null || target.isEmpty()) {
            res.sendError(404, "Product not found");
            return;
        }

        // 4) 302 리다이렉트
        res.sendRedirect(target);
    }

    // ===== helpers =====

    /** app.origin 끝의 슬래시 제거 */
    private String base() {
        return appOrigin.endsWith("/") ? appOrigin.substring(0, appOrigin.length() - 1) : appOrigin;
    }

    /** 실제 상품 URL 조회 (임시 구현) */
    private String resolveProductTargetUrl(String id) {
        // TODO: productService.findSpurlById(id)
        return "https://example-merchant.com/products/" + id;
    }

    /** 경로 세그먼트 안전 인코딩 (공백→%20, 슬래시 등 안전 처리) */
    private static String urlEncodePathSeg(String s) {
        return UriUtils.encodePathSegment(s, StandardCharsets.UTF_8);
    }

    /** HEX 문자열 상수시간 비교 */
    private static boolean constantTimeEqualsHex(String hexA, String hexB) {
        byte[] a = hexToBytes(hexA);
        byte[] b = hexToBytes(hexB);
        return MessageDigest.isEqual(a, b);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if ((len & 1) == 1) throw new IllegalArgumentException("odd-length hex");
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(2 * i), 16);
            int lo = Character.digit(hex.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("non-hex");
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
    
//    @GetMapping("/test")
//    public ResponseEntity<String> shareTest() {
//        return ResponseEntity.ok("Share API (hex-HMAC) is working!");
//    }
//}