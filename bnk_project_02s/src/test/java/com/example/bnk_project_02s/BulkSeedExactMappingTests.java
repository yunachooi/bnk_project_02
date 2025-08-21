package com.example.bnk_project_02s;

import com.example.bnk_project_02s.dto.UserDto;
import com.example.bnk_project_02s.entity.Review;
import com.example.bnk_project_02s.repository.ReviewRepository;
import com.example.bnk_project_02s.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.mindrot.jbcrypt.BCrypt;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // 테스트 인스턴스 공유(리뷰 NPE/Random bound fix)
@TestPropertySource(properties = {
        "aes.key.256=0123456789ABCDEF0123456789ABCDEF",             // 테스트용 키(운영X)
        "hmac.secret=super-secret-hmac-key-for-tests-32bytes!!"     // 테스트용 키(운영X)
})
class BulkSeedExactMappingTests {

    private static final Logger log = LoggerFactory.getLogger(BulkSeedExactMappingTests.class);
    private static final SecureRandom RND = new SecureRandom();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] CURRS = {"CNH","JPY","KRW","CHF","GBP","USD","EUR"};
    private static final String[] INTEREST_ENUM = {"TRAVEL","STUDY","SHOPPING","FINANCE","ETC"}; // ✅ 요구 enum
    private static final String[] NAMES_FAMILY = {"김","이","박","최","정","조","강","윤","장","임","한","오","서","신","권"};
    private static final String[] NAMES_GIVEN1 = {"민","서","도","하","지","아","유","준","현","윤","성","건","재","시","수"};
    private static final String[] NAMES_GIVEN2 = {"현","윤","우","호","린","혁","원","주","영","연","훈","율","연","희","빈"};
    private static final String[] CITIES = {"서울","부산","대구","광주","대전","울산","세종","제주","수원","창원"};
    private static final String[] REVIEW_TEXTS = {
            "사용하기 편하고 환전이 빨라요.","수수료가 저렴해서 좋아요.","UI/UX가 깔끔합니다.","알림이 유용해요.",
            "고객센터가 친절했어요.","안정성이 좋아졌습니다.","해외결제 연동이 편리합니다.","전체적으로 만족합니다.","별로예요.","그럭저럭 쓸만합니다."
    };

    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationContext ctx;

    @Autowired(required = false) UserService userService;
    @Autowired(required = false) ReviewRepository reviewRepository;

    private final List<String> createdUids = new ArrayList<>();

    /* -------------------- 공통: uid 확보(보강) -------------------- */
    private void ensureUserUidsLoaded() {
        if (!createdUids.isEmpty()) return;
        List<String> fromDb = jdbc.query("SELECT uid FROM bnk_user2", (rs, i) -> rs.getString(1));
        createdUids.addAll(fromDb);
        log.info("loaded {} uids from DB", createdUids.size());
    }

    /* ============================= 1) 사용자 300 ============================= */
    @Test @Order(1) @Transactional @Commit
    void seedUsers300() throws Exception {
        for (int i = 1; i <= 300; i++) {
            String uid = "dash_user_" + i;
            String uname = randomKorName();
            boolean male = RND.nextBoolean();
            int decade = new int[]{10,20,30,40,50,60}[(i-1)%6];

            String rrn = makeRrnForDecade(decade, male);
            String phone = makePhoneUnique();
            String ucurrencyCsv = pickCsv(CURRS, 2 + RND.nextInt(3));
            String uinterestCsv = pickCsv(INTEREST_ENUM, 1 + RND.nextInt(3)); // ✅ enum에서 1~3개
            String ugender = male ? "M" : "F";
            String ubirth = deriveBirth(rrn);

            String encPhone = aesEncrypt(phone);
            String hmacPhone = hmacAny(phone);
            String encRrn = aesEncrypt(rrn);
            String hmacRrn = hmacAny(rrn);
            String encPw = bcrypt("pw@" + i);

            String urole = "USER";
            String ucheck = RND.nextBoolean() ? "Y" : "N";
            long   ushare = RND.nextInt(5_000);

            // ✅ upush / upushdate 규칙: Y → 날짜 필수, N → null
            String upush  = RND.nextBoolean() ? "Y" : "N";
            String upushdate = upush.equals("Y") ? randomPast().format(FMT) : null;

            String ulocation = CITIES[RND.nextInt(CITIES.length)];

            boolean savedViaService = false;
            if (userService != null) {
                try {
                    UserDto dto = new UserDto();
                    setIfPresent(dto, "setUid", String.class, uid);
                    setIfPresent(dto, "setUpw", String.class, "pw@" + i);
                    setIfPresent(dto, "setUname", String.class, uname);

                    if (!setIfPresent(dto, "setRrn", String.class, rrn)) {
                        setIfPresent(dto, "setRrnFront", String.class, rrn.substring(0,6));
                        setIfPresent(dto, "setRrnBack",  String.class, rrn.substring(6));
                    }
                    if (!setIfPresent(dto, "setUphone", String.class, phone))
                        setIfPresent(dto, "setPhone", String.class, phone);

                    // 관심 통화/관심사: List/CSV 모두 대응
                    if (!setIfPresent(dto, "setUcurrency", List.class, Arrays.asList(ucurrencyCsv.split(","))))
                        setIfPresent(dto, "setUcurrency", String.class, ucurrencyCsv);
                    if (!setIfPresent(dto, "setUinterest", List.class, Arrays.asList(uinterestCsv.split(","))))
                        setIfPresent(dto, "setUinterest", String.class, uinterestCsv);

                    userService.signup(dto); // 내부서 암호화 경로
                    savedViaService = true;

                    // 서비스 경로로 들어간 경우 bnk_user2 확장칼럼 업데이트
                    tryUpdateExtraUserColumns(uid, ugender, ubirth, urole, ucheck, ushare,
                            upush, upushdate, ulocation, ucurrencyCsv, uinterestCsv);
                } catch (Exception e) {
                    log.warn("signup 경로 실패 → 직접 INSERT로 전환: {}", e.getMessage());
                }
            }

            if (!savedViaService) {
                jdbc.update("""
                        INSERT INTO bnk_user2
                        (uid, upw, uname, ugender, ubirth, uphoneEnc, uphoneHmac,
                         urrnEnc, urrnHmac, urole, ucurrency, uinterest,
                         ucheck, ushare, upush, upushdate, ulocation)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        """,
                        uid, encPw, uname, ugender, ubirth, encPhone, hmacPhone,
                        encRrn, hmacRrn, urole, ucurrencyCsv, uinterestCsv,
                        ucheck, ushare, upush, upushdate, ulocation
                );
            }

            createdUids.add(uid);
            if (i % 50 == 0) log.info("users: {}/300", i);
        }
        log.info("✅ 사용자 300명 완료");
    }

    /* ============================= 2) 리뷰 100 ============================= */
    @Test @Order(2) @Transactional @Commit
    void seedReviews100() {
        ensureUserUidsLoaded(); // ★ 인스턴스/재실행 대비
        int max = jdbc.queryForObject("SELECT COALESCE(MAX(CAST(rvno AS SIGNED)),0) FROM bnk_review", Integer.class);
        for (int i = 1; i <= 100; i++) {
            String rvno = String.valueOf(max + i);
            String uid  = createdUids.get(RND.nextInt(createdUids.size()));
            String content = REVIEW_TEXTS[RND.nextInt(REVIEW_TEXTS.length)];
            String rating  = String.valueOf(3 + RND.nextInt(3)); // 3~5
            String rdate   = randomPast().format(FMT);

            if (reviewRepository != null) {
                Review r = Review.builder()
                        .rvno(rvno).uid(uid).rvcontent(content).rvrating(rating).rvdate(rdate).build();
                reviewRepository.save(r);
            } else {
                jdbc.update("INSERT INTO bnk_review(rvno,uid,rvcontent,rvrating,rvdate) VALUES (?,?,?,?,?)",
                        rvno, uid, content, rating, rdate);
            }
        }
        log.info("✅ 리뷰 100개 완료");
    }

    /* ============================= 3) 거래내역 300 ============================= */
    @Test @Order(3) @Transactional @Commit
    void seedHistory300() {
        ensureUserUidsLoaded(); // ★ 보강
        Long max = jdbc.queryForObject("SELECT COALESCE(MAX(hno),0) FROM bnk_history", Long.class);
        long hno = (max == null ? 0L : max);

        Map<String, Long> balances = new HashMap<>();

        for (int i = 1; i <= 300; i++) {
            String uid = createdUids.get(RND.nextInt(createdUids.size()));
            String pano = "110-" + (10000000 + RND.nextInt(90000000));
            String cuno = CURRS[RND.nextInt(CURRS.length)];

            boolean deposit = RND.nextBoolean(); // 입금/출금
            long amount = 10_000L + RND.nextInt(3_000_000);
            long before = balances.getOrDefault(uid, 100_000L + RND.nextInt(900_000));
            long after  = deposit ? (before + amount) : Math.max(0, before - amount);

            String hwithdraw = deposit ? "0" : String.valueOf(amount);
            String hdeposit  = deposit ? String.valueOf(amount) : "0";
            String hbalance  = String.valueOf(after);
            String hkrw      = deposit ? "0" : String.valueOf((long) (amount * (0.9 + RND.nextDouble()*0.3)));

            String hdate     = randomPast().format(FMT);

            jdbc.update("""
                INSERT INTO bnk_history
                (hno, pano, cuno, uid, hwithdraw, hdeposit, hbalance, hkrw, hdate)
                VALUES (?,?,?,?,?,?,?,?,?)
            """, ++hno, pano, cuno, uid, hwithdraw, hdeposit, hbalance, hkrw, hdate);

            balances.put(uid, after);

            if (i % 50 == 0) log.info("history: {}/300", i);
        }
        log.info("✅ 거래내역 300건 완료");
    }

    /* ============================= 암호화/해시/BCrypt ============================= */

    private String bcrypt(String raw) throws Exception {
        // 1) PasswordEncoder 빈이 있으면 그걸 사용
        try {
            Object enc = ctx.getBean("passwordEncoder");
            Method m = enc.getClass().getMethod("encode", CharSequence.class);
            return String.valueOf(m.invoke(enc, raw));
        } catch (Exception ignore) {
            // 2) 없으면 jBCrypt 사용 (build.gradle에 org.mindrot:jbcrypt 있음)
            return BCrypt.hashpw(raw, BCrypt.gensalt(10));
        }
    }

    private String aesEncrypt(String plain) {
        for (String name : ctx.getBeanDefinitionNames()) {
            Object bean = ctx.getBean(name);
            for (Method m : bean.getClass().getMethods()) {
                if (m.getName().equalsIgnoreCase("encrypt")
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class
                        && m.getReturnType() == String.class) {
                    try { return String.valueOf(m.invoke(bean, plain)); }
                    catch (Exception ignored) {}
                }
            }
        }
        return plain; // encrypt 빈이 없으면 평문 저장(테스트 최소보장)
    }

    private String hmacAny(String plain) throws Exception {
        for (String name : ctx.getBeanDefinitionNames()) {
            Object bean = ctx.getBean(name);
            for (Method m : bean.getClass().getMethods()) {
                boolean nameOk = m.getName().equalsIgnoreCase("hmac") || m.getName().equalsIgnoreCase("hash");
                if (nameOk && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class
                        && m.getReturnType() == String.class) {
                    try { return String.valueOf(m.invoke(bean, plain)); }
                    catch (Exception ignored) {}
                }
            }
        }
        String secret = ctx.getEnvironment().getProperty("hmac.secret", "local-test-hmac-secret");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /* ============================= 보조 유틸 ============================= */

    private static String randomKorName() {
        return NAMES_FAMILY[RND.nextInt(NAMES_FAMILY.length)]
                + NAMES_GIVEN1[RND.nextInt(NAMES_GIVEN1.length)]
                + NAMES_GIVEN2[RND.nextInt(NAMES_GIVEN2.length)];
    }

    /** decade(10,20,...,60) + 성별 → 주민번호 13자리 생성 (형식만 유효) */
    private static String makeRrnForDecade(int decade, boolean male) {
        int year = switch (decade) {
            case 10, 20 -> 2000 + RND.nextInt(15);      // 2000~2014
            default     -> 1960 + RND.nextInt(40);      // 1960~1999
        };
        int yy = year % 100;
        int mm = 1 + RND.nextInt(12);
        int dd = 1 + RND.nextInt(28);
        int g  = (year >= 2000) ? (male ? 3 : 4) : (male ? 1 : 2);
        return String.format("%02d%02d%02d%d%06d", yy, mm, dd, g, RND.nextInt(1_000_000));
    }

    private static String deriveBirth(String rrn13) {
        String yy = rrn13.substring(0,2);
        String mm = rrn13.substring(2,4);
        String dd = rrn13.substring(4,6);
        int g = rrn13.charAt(6) - '0';
        String century = (g==1||g==2) ? "19" : "20";
        return century + yy + "-" + mm + "-" + dd;
    }

    private static String makePhoneUnique() {
        return "010" + String.format("%08d", RND.nextInt(100_000_000));
    }

    private static String pickCsv(String[] pool, int n) {
        List<String> xs = new ArrayList<>(Arrays.asList(pool));
        Collections.shuffle(xs, RND);
        return String.join(",", xs.subList(0, Math.min(n, xs.size())));
    }

    private static LocalDateTime randomPast() {
        return LocalDateTime.now()
                .minusDays(RND.nextInt(120))
                .withHour(RND.nextInt(24))
                .withMinute(RND.nextInt(60))
                .withSecond(RND.nextInt(60));
    }

    private void tryUpdateExtraUserColumns(
            String uid, String ugender, String ubirth, String urole, String ucheck, long ushare,
            String upush, String upushdate, String ulocation, String ucurrencyCsv, String uinterestCsv) {

        try {
            Integer cnt = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                            "WHERE table_name='bnk_user2' AND column_name='ugender'", Integer.class);
            if (cnt != null && cnt > 0) {
                jdbc.update("""
                    UPDATE bnk_user2 SET
                     ugender=?, ubirth=?, urole=?, ucurrency=?, uinterest=?, ucheck=?, ushare=?, upush=?, upushdate=?, ulocation=?
                    WHERE uid=?
                """, ugender, ubirth, urole, ucurrencyCsv, uinterestCsv,
                        ucheck, ushare, upush, upushdate, ulocation, uid);
            }
        } catch (Exception ignore) { }
    }

    private static boolean setIfPresent(Object target, String setter, Class<?> paramType, Object value) {
        try {
            Method m = target.getClass().getMethod(setter, paramType);
            Object v = value;
            if (paramType == String.class && !(value instanceof String)) v = String.valueOf(value);
            if (paramType == List.class && value instanceof String s) v = Arrays.asList(s.split(","));
            m.invoke(target, v);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
