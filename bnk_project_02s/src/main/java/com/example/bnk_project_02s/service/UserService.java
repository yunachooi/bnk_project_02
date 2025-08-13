package com.example.bnk_project_02s.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.bnk_project_02s.dto.UserDto;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.UserRepository;
import com.example.bnk_project_02s.util.UserUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserUtil userUtil;

    /* ===================== 조회/검증 ===================== */

    public boolean existsByUid(String uid) {
        return StringUtils.hasText(uid) && userRepository.existsByUid(uid);
    }

    public boolean isValidRrn(String front, String back) {
        String norm = userUtil.normalizeRrnParts(front, back);
        return userUtil.isValidRrn13(norm);
    }

    public boolean isRrnDuplicate(String front, String back) {
        String norm = userUtil.normalizeRrnParts(front, back);
        if (!userUtil.isValidRrn13(norm)) return false;
        String hmac = userUtil.hmacRrnHex(norm);
        return userRepository.existsByUrrnHmac(hmac);
    }

    public boolean isValidPhone(String phone) {
        String n = userUtil.normalizePhone(phone);
        return userUtil.isValidPhone(n);
    }

    public boolean isPhoneDuplicate(String phone) {
        String n = userUtil.normalizePhone(phone);
        if (!userUtil.isValidPhone(n)) return false;
        String h = userUtil.hmacPhoneHex(n);
        return userRepository.existsByUphoneHmac(h);
    }

    /* ===================== 가입 ===================== */

    @Transactional
    public String signup(UserDto dto) {
        // 1) 비밀번호 해시
        String hashed = StringUtils.hasText(dto.getUpw())
                ? BCrypt.hashpw(dto.getUpw(), BCrypt.gensalt(12))
                : null;

        // 2) 주민번호 정규화
        String rrnNorm = StringUtils.hasText(dto.getRrn())
                ? userUtil.normalizeRrn(dto.getRrn())
                : userUtil.normalizeRrnParts(dto.getRrnFront(), dto.getRrnBack());

        // 3) 주민번호 형식 강제 검증 (rrn_enc NOT NULL 대응)
        if (!userUtil.isValidRrn13(rrnNorm)) {
            throw new IllegalArgumentException("주민등록번호 형식 오류(앞 6자리 + 뒤 7자리).");
        }

        // 3-1) 성별/생년월 유도
        String ugender = deriveGenderFromRrn(rrnNorm); // 'M' / 'F' or null
        String ubirth  = deriveBirthFromRrn(rrnNorm);  // YYYY-MM-DD or null

        // 4) 주민번호 중복 체크(HMAC)
        String rrnHmacForCheck = userUtil.hmacRrnHex(rrnNorm);
        if (userRepository.existsByUrrnHmac(rrnHmacForCheck)) {
            throw new IllegalArgumentException("이미 등록된 주민등록번호입니다.");
        }

        // 5) 휴대폰: 정규화 → 형식 검증 → 중복 체크(HMAC) (선택값)
        String phoneNorm = null;
        if (StringUtils.hasText(dto.getUphone())) {
            phoneNorm = userUtil.normalizePhone(dto.getUphone());
            if (!userUtil.isValidPhone(phoneNorm)) {
                throw new IllegalArgumentException("휴대폰 형식 오류(01로 시작, 10~11자리).");
            }
            String phoneHmacForCheck = userUtil.hmacPhoneHex(phoneNorm);
            if (userRepository.existsByUphoneHmac(phoneHmacForCheck)) {
                throw new IllegalArgumentException("이미 등록된 휴대전화번호입니다.");
            }
        }

        // 6) 엔티티 매핑
        User u = new User();
        u.setUid(dto.getUid());
        u.setUpw(hashed);
        u.setUname(dto.getUname());
        u.setUgender(StringUtils.hasText(ugender) ? ugender : "M");
        u.setUbirth(ubirth);
        u.setUrole("ROLE_USER");
        u.setUcheck("N");
        u.setUshare(0L);

        // 목록 → CSV
        u.setUcurrency(dto.getUcurrency() != null && !dto.getUcurrency().isEmpty()
                ? String.join(",", dto.getUcurrency()) : null);
        u.setUinterest(dto.getUinterest() != null && !dto.getUinterest().isEmpty()
                ? String.join(",", dto.getUinterest()) : null);

        // 7) AES-GCM 컨버터 + HMAC 리스너 자동 처리(핵심 4줄)
        u.setUrrnEnc(rrnNorm);        // 평문 → @Convert(AES-GCM)가 DB 저장 시 암호화
        u.setRrnPlain(rrnNorm);       // 평문 → @HmacOf 리스너가 urrnHmac 자동 세팅

        if (phoneNorm != null) {
            u.setUphoneEnc(phoneNorm);   // 평문 → 컨버터가 암호화
            u.setPhonePlain(phoneNorm);  // 평문 → 리스너가 uphoneHmac 자동 세팅
        } else {
            u.setUphoneEnc(null);
            u.setPhonePlain(null);
        }

        // 🔧 HOTFIX: 리스너 실패 대비 — 서비스에서 한 번 더 직접 세팅 (NULL 방지)
        u.setUrrnHmac(userUtil.hmacRrnHex(rrnNorm));
        if (phoneNorm != null) {
            u.setUphoneHmac(userUtil.hmacPhoneHex(phoneNorm));
        }

        try {
            return userRepository.save(u).getUid();
        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약 위반(주민번호/휴대폰 HMAC 등)
            throw new IllegalArgumentException("이미 등록된 정보가 있습니다.");
        }
    }

    /* ===================== 인증 ===================== */

    @Transactional(readOnly = true)
    public User authenticate(String uid, String rawPw) {
        if (!StringUtils.hasText(uid) || !StringUtils.hasText(rawPw)) return null;

        Optional<User> opt = userRepository.findByUid(uid);
        if (opt.isEmpty()) return null;

        User u = opt.get();
        if (!StringUtils.hasText(u.getUpw())) return null;

        return BCrypt.checkpw(rawPw, u.getUpw()) ? u : null;
    }

    /* ===================== Helpers ===================== */

    private String deriveGenderFromRrn(String rrnDigits13) {
        if (rrnDigits13 == null || rrnDigits13.length() != 13) return null;
        char g = rrnDigits13.charAt(6);
        return switch (g) {
            case '1','3','5','7' -> "M"; // 내국/외국 남
            case '2','4','6','8' -> "F"; // 내국/외국 여
            default -> null;
        };
    }

    /** 생년월일 "YYYY-MM-DD" — 내/외국인 구분(1,2,5,6: 19xx / 3,4,7,8: 20xx) */
    private String deriveBirthFromRrn(String rrnDigits13) {
        if (rrnDigits13 == null || rrnDigits13.length() != 13) return null;
        String yy = rrnDigits13.substring(0, 2);
        String mm = rrnDigits13.substring(2, 4);
        String dd = rrnDigits13.substring(4, 6);
        char g = rrnDigits13.charAt(6);

        String century = switch (g) {
            case '1','2','5','6' -> "19";
            case '3','4','7','8' -> "20";
            default -> null;
        };
        if (century == null) return null;
        return century + yy + "-" + mm + "-" + dd;
    }

    /** 알림푸쉬 권한 관련 */
    @Transactional
    public void updatePushConsent(String uid, boolean consent) {
        User u = userRepository.findById(uid)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음: " + uid));
        u.setUpush(consent ? "Y" : "N");
        String nowKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        u.setUpushdate(nowKst);
        userRepository.save(u);
    }
}