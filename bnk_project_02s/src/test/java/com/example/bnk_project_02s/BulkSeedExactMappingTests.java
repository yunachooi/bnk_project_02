package com.example.bnk_project_02s;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.dto.UserDto;
import com.example.bnk_project_02s.entity.Review;
import com.example.bnk_project_02s.repository.ReviewRepository;
import com.example.bnk_project_02s.service.UserService;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "aes.key.256=0123456789ABCDEF0123456789ABCDEF",             // 테스트용 키(운영X)
    "hmac.secret=super-secret-hmac-key-for-tests-32bytes!!"     // 테스트용 키(운영X)
})
class BulkSeedExactMappingTests {

    private static final Logger log = LoggerFactory.getLogger(BulkSeedExactMappingTests.class);
    private static final SecureRandom RND = new SecureRandom();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // FK/흐름 보장용 메모리 캐시
    private final Map<String, List<String>> uidToPanos = new HashMap<>();  // uid -> 부모계좌들
    private final List<String> createdUids = new ArrayList<>();

    // 통화코드 (bnk_currency: cuno, cuname)
    private static record Cur(String cuno, String cuname) {}
    private final List<Cur> currencyList = new ArrayList<>();

    // 샘플 풀
    private static final String[] CURRS_FALLBACK = {"156:CNH","392:JPY","410:KRW","756:CHF","826:GBP","840:USD","978:EUR"};
    private static final String[] INTEREST_ENUM = {"TRAVEL","STUDY","SHOPPING","FINANCE","ETC"};
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

    /* -----------------------------------------------------------
     *  공통 유틸
     * ----------------------------------------------------------- */

    // --- 스키마 도우미 ---
    private boolean hasColumn(String table, String column) {
        Integer cnt = 0;
        try {
            cnt = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
            """, Integer.class, table, column);
        } catch (Exception ignore) {}
        return cnt != null && cnt > 0;
    }
    private String columnType(String table, String column) {
        try {
            return jdbc.queryForObject("""
                SELECT LOWER(DATA_TYPE)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?""",
                String.class, table, column);
        } catch (Exception e) { return null; }
    }
    private boolean isNullable(String table, String column) {
        try {
            String v = jdbc.queryForObject("""
                SELECT IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?""",
                String.class, table, column);
            return "YES".equalsIgnoreCase(v);
        } catch (Exception e) { return true; }
    }
    private boolean isNumericType(String t) {
        if (t == null) return false;
        return t.matches("^(int|bigint|smallint|mediumint|tinyint|decimal|numeric|double|float)$");
    }
    private boolean tableExists(String table) {
        Integer n = 0;
        try {
            n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?""", Integer.class, table);
        } catch (Exception ignore) {}
        return n != null && n > 0;
    }
    private Integer fetchOneInt(String sql) {
        try { return jdbc.queryForObject(sql, Integer.class); }
        catch (Exception e) { return null; }
    }

    private void ensureUserUidsLoaded() {
        if (!createdUids.isEmpty()) return;
        createdUids.addAll(jdbc.query("SELECT uid FROM bnk_user2", (rs,i)->rs.getString(1)));
        log.info("loaded {} uids from DB", createdUids.size());
    }

    private void ensureParentAccountsLoaded() {
        // DB에 이미 있는 부모계좌 적재
        jdbc.query("SELECT uid, pano FROM bnk_parent_account", rs -> {
            uidToPanos.computeIfAbsent(rs.getString("uid"), k->new ArrayList<>()).add(rs.getString("pano"));
        });
    }

    private void ensureCurrenciesLoaded() {
        if (!currencyList.isEmpty()) return;
        try {
            jdbc.query("SELECT CAST(cuno AS CHAR), cuname FROM bnk_currency", rs -> {
                currencyList.add(new Cur(rs.getString(1), rs.getString(2)));
            });
        } catch (Exception ignore) {}
        if (currencyList.isEmpty()) {
            for (String s : CURRS_FALLBACK) {
                String[] a = s.split(":");
                currencyList.add(new Cur(a[0], a[1]));
            }
        }
    }

    private static String randomKorName() {
        return NAMES_FAMILY[RND.nextInt(NAMES_FAMILY.length)]
                + NAMES_GIVEN1[RND.nextInt(NAMES_GIVEN1.length)]
                + NAMES_GIVEN2[RND.nextInt(NAMES_GIVEN2.length)];
    }

    private static String makePhoneUnique() { return "010" + String.format("%08d", RND.nextInt(100_000_000)); }

    private static String pickCsv(String[] pool, int n) {
        List<String> xs = new ArrayList<>(Arrays.asList(pool));
        Collections.shuffle(xs, RND);
        return String.join(",", xs.subList(0, Math.min(n, xs.size())));
    }

    private static LocalDateTime randomPast() {
        return LocalDateTime.now()
                .minusDays(RND.nextInt(120))
                .withHour(RND.nextInt(24)).withMinute(RND.nextInt(60)).withSecond(RND.nextInt(60));
    }

    /** 주민번호 13자리 생성 (연령대/성별 기반) */
    private static String makeRrnForDecade(int decade, boolean male) {
        int year = switch (decade) { case 10,20 -> 2000 + RND.nextInt(15); default -> 1960 + RND.nextInt(40); };
        int yy = year % 100, mm = 1 + RND.nextInt(12), dd = 1 + RND.nextInt(28);
        int g  = (year >= 2000) ? (male ? 3 : 4) : (male ? 1 : 2);
        return String.format("%02d%02d%02d%d%06d", yy, mm, dd, g, RND.nextInt(1_000_000));
    }

    private static String deriveBirth(String rrn13) {
        int g = rrn13.charAt(6) - '0';
        String century = (g==1||g==2) ? "19" : "20";
        return century + rrn13.substring(0,2) + "-" + rrn13.substring(2,4) + "-" + rrn13.substring(4,6);
    }

    private String bcrypt(String raw) throws Exception {
        try {
            Object enc = ctx.getBean("passwordEncoder");
            Method m = enc.getClass().getMethod("encode", CharSequence.class);
            return String.valueOf(m.invoke(enc, raw));
        } catch (Exception ignore) { return BCrypt.hashpw(raw, BCrypt.gensalt(10)); }
    }

    private String aesEncrypt(String plain) {
        for (String name : ctx.getBeanDefinitionNames()) {
            Object bean = ctx.getBean(name);
            for (Method m : bean.getClass().getMethods()) {
                if (m.getName().equalsIgnoreCase("encrypt")
                    && m.getParameterCount()==1 && m.getParameterTypes()[0]==String.class
                    && m.getReturnType()==String.class) {
                    try { return String.valueOf(m.invoke(bean, plain)); } catch (Exception ignored) {}
                }
            }
        }
        return plain; // encrypt 빈이 없으면 평문 저장(테스트 최소보장)
    }

    private String hmacAny(String plain) throws Exception {
        for (String name : ctx.getBeanDefinitionNames()) {
            Object bean = ctx.getBean(name);
            for (Method m : bean.getClass().getMethods()) {
                boolean ok = m.getName().equalsIgnoreCase("hmac") || m.getName().equalsIgnoreCase("hash");
                if (ok && m.getParameterCount()==1 && m.getParameterTypes()[0]==String.class
                        && m.getReturnType()==String.class) {
                    try { return String.valueOf(m.invoke(bean, plain)); } catch (Exception ignored) {}
                }
            }
        }
        String secret = ctx.getEnvironment().getProperty("hmac.secret", "local-test-hmac-secret");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(); for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /* -----------------------------------------------------------
     *  넘버 생성기
     * ----------------------------------------------------------- */
    private static String newPano() { return "110-" + (10000000 + RND.nextInt(90000000)); } // 부모계좌
    private static String newCano() { return "CA"  + (100000000 + RND.nextInt(900000000)); } // 자식계좌
    private static String newCardNo() { StringBuilder sb=new StringBuilder(16); for(int i=0;i<16;i++) sb.append(RND.nextInt(10)); return sb.toString(); }
    private static int newCvc() { return 100 + RND.nextInt(900); }

    /* -----------------------------------------------------------
     *  1) 사용자 300
     * ----------------------------------------------------------- */
    @Test @Order(1) @Transactional @Commit
    void seedUsers300() throws Exception {
        for (int i = 1; i <= 300; i++) {
            String uid = "dash_user_" + i;
            String uname = randomKorName();
            boolean male = RND.nextBoolean();
            int decade = new int[]{10,20,30,40,50,60}[(i-1)%6];

            String rrn = makeRrnForDecade(decade, male);
            String phone = makePhoneUnique();
            String ucurrencyCsv = pickCsv(Arrays.stream(CURRS_FALLBACK).map(s->s.split(":")[1]).toArray(String[]::new),
                                          2 + RND.nextInt(3));
            String uinterestCsv = pickCsv(INTEREST_ENUM, 1 + RND.nextInt(3));
            String ugender = male ? "M" : "F";
            String ubirth  = deriveBirth(rrn);

            String encPhone = aesEncrypt(phone), hmacPhone = hmacAny(phone);
            String encRrn = aesEncrypt(rrn),     hmacRrn = hmacAny(rrn);
            String encPw  = bcrypt("pw@" + i);

            String urole = "USER";
            String ucheck = RND.nextBoolean() ? "Y" : "N";
            long   ushare = RND.nextInt(5_000);
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
                    if (!setIfPresent(dto, "setUcurrency", List.class, Arrays.asList(ucurrencyCsv.split(","))))
                        setIfPresent(dto, "setUcurrency", String.class, ucurrencyCsv);
                    if (!setIfPresent(dto, "setUinterest", List.class, Arrays.asList(uinterestCsv.split(","))))
                        setIfPresent(dto, "setUinterest", String.class, uinterestCsv);

                    userService.signup(dto);
                    savedViaService = true;

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
                """, uid, encPw, uname, ugender, ubirth, encPhone, hmacPhone,
                     encRrn, hmacRrn, urole, ucurrencyCsv, uinterestCsv,
                     ucheck, ushare, upush, upushdate, ulocation);
            }
            createdUids.add(uid);
            if (i % 50 == 0) log.info("users: {}/300", i);
        }
        log.info("✅ 사용자 300명 완료");
    }

    /* -----------------------------------------------------------
     *  2) 리뷰 100
     * ----------------------------------------------------------- */
    @Test @Order(2) @Transactional @Commit
    void seedReviews100() {
        ensureUserUidsLoaded();
        int max = jdbc.queryForObject("SELECT COALESCE(MAX(CAST(rvno AS SIGNED)),0) FROM bnk_review", Integer.class);
        for (int i = 1; i <= 100; i++) {
            String rvno = String.valueOf(max + i);
            String uid  = createdUids.get(RND.nextInt(createdUids.size()));
            String content = REVIEW_TEXTS[RND.nextInt(REVIEW_TEXTS.length)];
            String rating  = String.valueOf(3 + RND.nextInt(3)); // 3~5
            String rdate   = randomPast().format(FMT);

            if (reviewRepository != null) {
                Review r = Review.builder().rvno(rvno).uid(uid).rvcontent(content).rvrating(rating).rvdate(rdate).build();
                reviewRepository.save(r);
            } else {
                jdbc.update("INSERT INTO bnk_review(rvno,uid,rvcontent,rvrating,rvdate) VALUES (?,?,?,?,?)",
                        rvno, uid, content, rating, rdate);
            }
        }
        log.info("✅ 리뷰 100개 완료");
    }

    /* -----------------------------------------------------------
     *  3) 상품가입 → 부모/자식/카드 → 거래내역
     * ----------------------------------------------------------- */
    @Test @Order(3) @Transactional @Commit
    void seedProductSignupAndHistories() {
        ensureUserUidsLoaded();
        ensureParentAccountsLoaded();
        ensureCurrenciesLoaded();

        int signups = 200; // 생성할 상품가입 건수
        for (int i=1;i<=signups;i++) {
            String uid = createdUids.get(RND.nextInt(createdUids.size()));
            Cur cur    = currencyList.get(RND.nextInt(currencyList.size()));
            LocalDate join = randomPast().toLocalDate();

            // 1) 부모계좌
            String pano = insertParentAccount(uid, join);
            uidToPanos.computeIfAbsent(uid, k->new ArrayList<>()).add(pano);

            // 2) 자식(통화별)계좌
            String cano = insertChildAccount(pano, cur, join);

            // 3) 카드
            insertCard(cano, uid, join);

            // 4) 거래내역 (해당 통화로 2~5건)
            insertHistory(pano, uid, cur.cuno(), 2 + RND.nextInt(4));

            if (i % 25 == 0) log.info("signup flow: {}/{}", i, signups);
        }
        log.info("✅ 상품가입 {}건(부모/자식/카드/거래내역) 생성 완료", signups);
    }

    /* -----------------------------------------------------------
     *  INSERT helpers (스키마/타입 자동 감지)
     * ----------------------------------------------------------- */

    private String insertParentAccount(String uid, LocalDate joinDate) {
        String tFno    = columnType("bnk_parent_account", "fno");
        String tPabank = columnType("bnk_parent_account", "pabank");

        boolean hasFno    = tFno    != null;
        boolean hasPabank = tPabank != null;

        String cols = "pano, uid" + (hasFno? ", fno" : "") + ", pajoin" + (hasPabank? ", pabank" : "");
        String qs   =  "?,   ?"   + (hasFno? ", ?"  : "") + ", ?"     + (hasPabank? ", ?"     : "");
        String sql  = "INSERT INTO bnk_parent_account ("+cols+") VALUES ("+qs+")";

        String pano = newPano();
        List<Object> params = new ArrayList<>();
        params.add(pano);
        params.add(uid);

        // fno 값
        if (hasFno) {
            Object fnoVal;
            if (isNumericType(tFno)) {
                Integer v = tableExists("bnk_fx_product") ? fetchOneInt("SELECT MIN(fno) FROM bnk_fx_product") : null;
                if (v == null) v = fetchOneInt("SELECT MIN(fno) FROM bnk_parent_account WHERE fno IS NOT NULL");
                fnoVal = (v == null) ? (isNullable("bnk_parent_account","fno") ? null : 0) : v;
            } else {
                fnoVal = "FX-DEFAULT";
            }
            params.add(fnoVal);
        }

        params.add(Date.valueOf(joinDate));

        // pabank 값
        if (hasPabank) {
            Object bankVal;
            if (isNumericType(tPabank)) {
                Integer v = tableExists("bnk_bank") ? fetchOneInt("SELECT MIN(bankno) FROM bnk_bank") : null;
                bankVal = (v == null) ? (isNullable("bnk_parent_account","pabank") ? null : 1) : v;
            } else {
                bankVal = "MAIN";
            }
            params.add(bankVal);
        }

        jdbc.update(sql, params.toArray());
        return pano;
    }

    private String insertChildAccount(String pano, Cur cur, LocalDate joinDate) {
        String tPabank    = columnType("bnk_child_account", "pabank");
        String tCajoin    = columnType("bnk_child_account", "cajoin");
        String tCabalance = columnType("bnk_child_account", "cabalance");
        String tCuno      = columnType("bnk_child_account", "cuno");

        boolean hasPabank = tPabank != null;

        String cols = "cano, pano, cuno, cajoin, cabalance" + (hasPabank ? ", pabank" : "");
        String qs   =  "?,   ?,    ?,    ?,      ?"         + (hasPabank ? ", ?"     : "");
        String sql  = "INSERT INTO bnk_child_account ("+cols+") VALUES ("+qs+")";

        String cano = newCano();
        List<Object> params = new ArrayList<>();
        params.add(cano);
        params.add(pano);

        // cuno: 숫자 컬럼이면 정수로, 아니면 문자열
        params.add(isNumericType(tCuno) ? Integer.valueOf(cur.cuno()) : cur.cuno());

        // cajoin: DATE/DATETIME이면 Date, 아니면 문자열
        if ("date".equals(tCajoin) || "datetime".equals(tCajoin) || "timestamp".equals(tCajoin)) {
            params.add(Date.valueOf(joinDate));
        } else {
            params.add(joinDate.toString());
        }

        // cabalance: 숫자면 0, 아니면 "0"
        params.add(isNumericType(tCabalance) ? 0 : "0");

        if (hasPabank) {
            Object bankVal = isNumericType(tPabank)
                    ? (isNullable("bnk_child_account","pabank") ? null : 1)
                    : "MAIN";
            params.add(bankVal);
        }

        jdbc.update(sql, params.toArray());
        return cano;
    }

    private String insertCard(String cano, String uid, LocalDate cardDate) {
        String tStatus = columnType("bnk_card","cardstatus");

        String cols = "cardno, cano, uid, cardname, cardcvc, carddate" + (tStatus!=null ? ", cardstatus" : "");
        String qs   =  "?,      ?,   ?,   ?,        ?,       ?"        + (tStatus!=null ? ", ?"         : "");
        String sql  = "INSERT INTO bnk_card ("+cols+") VALUES ("+qs+")";

        String cardno = newCardNo();
        List<Object> params = new ArrayList<>();
        params.add(cardno);
        params.add(cano);
        params.add(uid);
        params.add("FX-" + (100 + RND.nextInt(900)));
        params.add(newCvc());
        params.add(Date.valueOf(cardDate));
        if (tStatus != null) params.add(isNumericType(tStatus) ? 1 : "Y");

        jdbc.update(sql, params.toArray());
        return cardno;
    }

    private void insertHistory(String pano, String uid, String cuno, int count) {
        String tCuno = columnType("bnk_history","cuno");
        boolean hasCuno = tCuno != null;

        String cols = "hno, pano" + (hasCuno ? ", cuno" : "") + ", uid, hwithdraw, hdeposit, hbalance, hkrw, hdate";
        String qs   =  "?,  ?"    + (hasCuno ? ", ?"   : "") + ", ?,   ?,         ?,        ?,      ?,     ?";
        String sql  = "INSERT INTO bnk_history ("+cols+") VALUES ("+qs+")";

        Long max = jdbc.queryForObject("SELECT COALESCE(MAX(hno),0) FROM bnk_history", Long.class);
        long hno = (max == null ? 0L : max);

        long balance = 0; // 자식계좌 초기 0원과 맞춤
        for (int i=0;i<count;i++) {
            boolean deposit = RND.nextBoolean();
            long amount = 10_000L + RND.nextInt(3_000_000);
            long after  = deposit ? (balance + amount) : Math.max(0, balance - amount);

            List<Object> params = new ArrayList<>();
            params.add(++hno);
            params.add(pano);
            if (hasCuno) params.add(isNumericType(tCuno) ? Integer.valueOf(cuno) : cuno);
            params.add(uid);
            params.add(deposit ? "0" : String.valueOf(amount));
            params.add(deposit ? String.valueOf(amount) : "0");
            params.add(String.valueOf(after));
            params.add(deposit ? "0" : String.valueOf((long)(amount * (0.9 + RND.nextDouble()*0.3))));
            params.add(randomPast().format(FMT));

            jdbc.update(sql, params.toArray());
            balance = after;
        }
    }

    /* -----------------------------------------------------------
     *  부가 유틸
     * ----------------------------------------------------------- */

    private void tryUpdateExtraUserColumns(
        String uid, String ugender, String ubirth, String urole, String ucheck, long ushare,
        String upush, String upushdate, String ulocation, String ucurrencyCsv, String uinterestCsv) {
        try {
            Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name='bnk_user2' AND column_name='ugender'",
                Integer.class);
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
