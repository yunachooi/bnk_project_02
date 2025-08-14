package com.example.bnk_project_02s.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.auth.CryptoBeans;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.UserRepository;
import com.example.bnk_project_02s.util.UserUtil;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@Slf4j
public class AdminUserApiController {

    private final UserRepository userRepository; // 쓰기용(JPA)
    private final UserUtil userUtil;             // 정규화 유틸
    private final JdbcTemplate jdbc;             // 조회 전용(컨버터 우회)

    /* ===== 내부 헬퍼: 평문이면 BCrypt 해시로 변환 ===== */
    private String bcryptIfNeeded(String pw) {
        if (!StringUtils.hasText(pw)) return null;
        String p = pw.trim();
        if (p.startsWith("$2a$") || p.startsWith("$2b$") || p.startsWith("$2y$")) {
            return p; // 이미 해시
        }
        return BCrypt.hashpw(p, BCrypt.gensalt(12));
    }

    /* 목록 */
    @GetMapping
    public Page<UserView> list(
            @RequestParam(name="page", defaultValue="0") int page,
            @RequestParam(name="size", defaultValue="10") int size,
            @RequestParam(name="q", required=false) String q,
            @RequestParam(name="phone", required=false) String phone
    ){
        int p = Math.max(page,0);
        int s = Math.min(size,100);
        int offset = p * s;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();

        if (StringUtils.hasText(phone)) {
            // 1) 동일한 정규화 로직 사용: 숫자만
            String digits = userUtil.normalizePhone(phone);  // "01011111111" 형태
            if (!StringUtils.hasText(digits)) {
                // 숫자가 하나도 안 남으면 q 검색으로 폴백
                if (StringUtils.hasText(q)) {
                    where.append(" AND (LOWER(u1.uid) LIKE LOWER(CONCAT('%', ?, '%')) " +
                                 "  OR LOWER(u1.uname) LIKE LOWER(CONCAT('%', ?, '%'))) ");
                    args.add(q);
                    args.add(q);
                }
            } else {
                // 2) 다양한 레거시 HMAC 가능성까지 매칭
                String dashed     = toDashed(digits);                       // "010-1111-1111"
                String hNew       = CryptoBeans.HMAC.hmacHex("phone:" + digits);
                String hDashed    = CryptoBeans.HMAC.hmacHex("phone:" + dashed);
                String hNoDomain  = CryptoBeans.HMAC.hmacHex(digits);       // 옛날: 접두사 없이 HMAC

                // 3) uphone(평문) 호환: 숫자만으로 동등 비교
                where.append(
                    " AND ( u1.phone_hmac IN (?, ?, ?) " +
                    "    OR REPLACE(REPLACE(REPLACE(COALESCE(u1.uphone,''),'-',''),' ',''),'.','') = ? ) "
                );
                args.add(hNew);
                args.add(hDashed);
                args.add(hNoDomain);
                args.add(digits);
            }

        } else if (StringUtils.hasText(q)) {
            where.append(" AND (LOWER(u1.uid) LIKE LOWER(CONCAT('%', ?, '%')) " +
                         "  OR LOWER(u1.uname) LIKE LOWER(CONCAT('%', ?, '%'))) ");
            args.add(q);
            args.add(q);
        }

        String selectSql = """
            SELECT
                u1.uid,
                u1.uname,
                u1.ugender,
                u1.ubirth,
                u1.rrn_enc,
                u1.phone_enc,
                u1.ucurrency,
                u1.uinterest,
                u1.ucheck,
                u1.ulocation,
                u1.upush,
                u1.upushdate
            FROM bnk_user2 u1
        """ + where + " ORDER BY u1.upushdate DESC, u1.uid ASC LIMIT ? OFFSET ?";

        String countSql = "SELECT COUNT(*) FROM bnk_user2 u1 " + where;

        long total = jdbc.queryForObject(countSql, args.toArray(), Long.class);

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(s);
        pageArgs.add(offset);

        List<UserView> content = jdbc.query(selectSql, pageArgs.toArray(), rowMapper());

        Pageable pageable = PageRequest.of(p, s);
        return new PageImpl<>(content, pageable, total);
    }

    /* 상세 */
    @GetMapping("/{uid}")
    public ResponseEntity<UserView> get(@PathVariable("uid") String uid){
        String sql = """
            SELECT
                u1.uid, u1.uname, u1.ugender, u1.ubirth,
                u1.rrn_enc, u1.phone_enc,
                u1.ucurrency, u1.uinterest, u1.ucheck, u1.ulocation,
                u1.upush, u1.upushdate
            FROM bnk_user2 u1
            WHERE u1.uid = ?
            """;
        List<UserView> list = jdbc.query(sql, new Object[]{uid}, rowMapper());
        return list.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(list.get(0));
    }

    /* 등록 */
    @PostMapping
    public ResponseEntity<UserView> create(@RequestBody SaveReq req){
        if (!StringUtils.hasText(req.getUid())) return ResponseEntity.badRequest().build();
        if (!StringUtils.hasText(req.getUpw())) return ResponseEntity.badRequest().build(); // upw 필수
        if (userRepository.existsById(req.getUid())) return ResponseEntity.status(409).build();
        // rrn_hmac NOT NULL → 주민번호 필수
        if (!StringUtils.hasText(req.getRrn())) return ResponseEntity.badRequest().build();

        User u = new User();
        u.setUid(req.getUid().trim().toLowerCase());
        u.setUpw(bcryptIfNeeded(req.getUpw())); // 반드시 해시 저장

        u.setUname(req.getUname());
        u.setUgender(req.getUgender());
        u.setUbirth(req.getUbirth());

        // 주민번호(필수): 평문 -> enc 저장 + HMAC 직접 세팅
        String rrn13 = userUtil.normalizeRrn(req.getRrn());
        u.setUrrnEnc(rrn13);                                    // AesGcmConverter가 암호화
        u.setUrrnHmac(CryptoBeans.HMAC.hmacHex("rrn:" + rrn13)); // HMAC 직접 세팅

        // 휴대폰(선택)
        if (req.getPhone() != null) {
            String p = userUtil.normalizePhone(req.getPhone());
            if (StringUtils.hasText(p)) {
                u.setUphone(p);                                     // (호환용 평문)
                u.setUphoneEnc(p);                                  // 컨버터가 암호화
                u.setUphoneHmac(CryptoBeans.HMAC.hmacHex("phone:" + p));
            } else {
                u.setUphone(null);
                u.setUphoneEnc(null);
                u.setUphoneHmac(null);
            }
        }

        // 조회용 부가 필드
        u.setUcurrency(req.getUcurrency());
        u.setUinterest(req.getUinterest());
        u.setUcheck(req.getUcheck());
        u.setUlocation(req.getUlocation());
        u.setUpush(req.getUpush());
        u.setUpushdate(req.getUpushdate());

        userRepository.save(u);
        return get(u.getUid());
    }

    /* 수정 */
    @PutMapping("/{uid}")
    public ResponseEntity<UserView> update(@PathVariable("uid") String uid, @RequestBody SaveReq req){
        return userRepository.findById(uid).map(u -> {
            if (StringUtils.hasText(req.getUpw()))     u.setUpw(bcryptIfNeeded(req.getUpw()));
            if (StringUtils.hasText(req.getUname()))   u.setUname(req.getUname());
            if (StringUtils.hasText(req.getUgender())) u.setUgender(req.getUgender());
            if (StringUtils.hasText(req.getUbirth()))  u.setUbirth(req.getUbirth());

            // 주민번호가 요청에 포함되었으면 재설정
            if (req.getRrn() != null) {
                String rrn13 = userUtil.normalizeRrn(req.getRrn());
                if (StringUtils.hasText(rrn13)) {
                    u.setUrrnEnc(rrn13);
                    u.setUrrnHmac(CryptoBeans.HMAC.hmacHex("rrn:" + rrn13));
                } else {
                    u.setUrrnEnc(null);
                    u.setUrrnHmac(null);
                }
            }

            // 휴대폰이 요청에 포함되었으면 재설정
            if (req.getPhone() != null) {
                String p = userUtil.normalizePhone(req.getPhone());
                if (StringUtils.hasText(p)) {
                    u.setUphone(p);
                    u.setUphoneEnc(p);
                    u.setUphoneHmac(CryptoBeans.HMAC.hmacHex("phone:" + p));
                } else {
                    u.setUphone(null);
                    u.setUphoneEnc(null);
                    u.setUphoneHmac(null);
                }
            }

            if (StringUtils.hasText(req.getUcurrency())) u.setUcurrency(req.getUcurrency());
            if (StringUtils.hasText(req.getUinterest())) u.setUinterest(req.getUinterest());
            if (StringUtils.hasText(req.getUcheck()))    u.setUcheck(req.getUcheck());
            if (StringUtils.hasText(req.getUlocation())) u.setUlocation(req.getUlocation());
            if (StringUtils.hasText(req.getUpush()))     u.setUpush(req.getUpush());
            if (StringUtils.hasText(req.getUpushdate())) u.setUpushdate(req.getUpushdate());

            userRepository.save(u);
            return get(uid);
        }).orElse(ResponseEntity.notFound().build());
    }

    /* 삭제 */
    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable("uid") String uid){
        if (!userRepository.existsById(uid)) return ResponseEntity.notFound().build();
        userRepository.deleteById(uid);
        return ResponseEntity.noContent().build();
    }

    /* ===== DTO ===== */
    @Data
    public static class SaveReq {
        private String uid;
        private String upw;
        private String uname;
        private String ugender;    // M/F
        private String ubirth;     // yyyy-MM-dd
        private String rrn;        // 주민번호(13자리 또는 6-7)
        private String phone;      // 숫자만
        private String ucurrency;
        private String uinterest;
        private String ucheck;
        private String ulocation;
        private String upush;
        private String upushdate;
    }

    @Data
    public static class UserView {
        private String uid;
        private String uname;
        private String ugender;
        private String ubirth;
        private String rrn;        // 복호화 표시
        private String phone;      // 복호화 표시
        private String ucurrency;
        private String uinterest;
        private String ucheck;
        private String ulocation;
        private String upush;
        private String upushdate;
    }

    /* ===== 조회용 RowMapper (JDBC로 직접 복호) ===== */
    private RowMapper<UserView> rowMapper(){
        return new RowMapper<UserView>() {
            @Override public UserView mapRow(ResultSet rs, int rowNum) throws SQLException {
                UserView v = new UserView();
                v.setUid(rs.getString("uid"));
                v.setUname(rs.getString("uname"));
                v.setUgender(rs.getString("ugender"));
                v.setUbirth(rs.getString("ubirth"));
                v.setUcurrency(rs.getString("ucurrency"));
                v.setUinterest(rs.getString("uinterest"));
                v.setUcheck(rs.getString("ucheck"));
                v.setUlocation(rs.getString("ulocation"));
                v.setUpush(rs.getString("upush"));
                v.setUpushdate(rs.getString("upushdate"));

                String rrnEnc   = rs.getString("rrn_enc");
                String phoneEnc = rs.getString("phone_enc");
                v.setRrn(fmtRrn(tryDecrypt(rrnEnc)));
                v.setPhone(fmtPhone(tryDecrypt(phoneEnc)));
                return v;
            }
        };
    }

    private String tryDecrypt(String val){
        if (!StringUtils.hasText(val)) return null;
        try { return CryptoBeans.AES.decrypt(val); }
        catch (Exception e) {
            log.debug("decrypt fail, show raw. head={}", val.substring(0, Math.min(10, val.length())));
            return val;
        }
    }
    private String fmtRrn(String rrn){
        if (!StringUtils.hasText(rrn)) return rrn;
        String d = rrn.replaceAll("\\D","");
        return d.length()==13 ? d.substring(0,6)+"-"+d.substring(6) : rrn;
    }
    private String fmtPhone(String p){
        if (!StringUtils.hasText(p)) return p;
        String d = p.replaceAll("\\D","");
        if (d.length()==11) return d.replaceFirst("^(\\d{3})(\\d{4})(\\d{4})$", "$1-$2-$3");
        if (d.length()==10) return d.replaceFirst("^(\\d{3})(\\d{3,4})(\\d{4})$", "$1-$2-$3");
        if (d.length()==9)  return d.replaceFirst("^(\\d{2})(\\d{3})(\\d{4})$", "$1-$2-$3");
        return p;
    }

    /** 레거시 HMAC 대응: 숫자를 대시 포맷으로 변환 */
    private String toDashed(String d){
        if (d == null) return null;
        String s = d.replaceAll("\\D","");
        if (s.length()==11) return s.replaceFirst("^(\\d{3})(\\d{4})(\\d{4})$", "$1-$2-$3");
        if (s.length()==10) return s.replaceFirst("^(\\d{3})(\\d{3,4})(\\d{4})$", "$1-$2-$3");
        if (s.length()==9)  return s.replaceFirst("^(\\d{2})(\\d{3})(\\d{4})$", "$1-$2-$3");
        return s;
    }
}
