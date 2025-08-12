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

    /** rrnFront+rrnBack 형식 검증 (숫자 6 + 7) */
    public boolean isValidRrn(String front, String back) {
        String norm = userUtil.normalizeRrnParts(front, back);
        return userUtil.isValidRrn13(norm);
    }

    /** 주민등록번호 중복 여부(HMAC로 조회) */
    public boolean isRrnDuplicate(String front, String back) {
        String norm = userUtil.normalizeRrnParts(front, back);
        if (!userUtil.isValidRrn13(norm)) return false; // 형식 틀리면 여기선 중복 판단 안 함
        String hmac = userUtil.hmacRrnHex(norm);
        return userRepository.existsByUrrnHmac(hmac);   // ← 레포지토리에 이 메서드가 있어야 함
    }

    /** 휴대번호 형식 검증(문자열 아무거나 받아도 내부에서 숫자만 비교) */
    public boolean isValidPhone(String phone) {
        String n = userUtil.normalizePhone(phone);
        return userUtil.isValidPhone(n);
    }

    /** 휴대번호 중복 여부(HMAC로 조회) */
    public boolean isPhoneDuplicate(String phone) {
        String n = userUtil.normalizePhone(phone);
        if (!userUtil.isValidPhone(n)) return false;
        String h = userUtil.hmacPhoneHex(n);
        return userRepository.existsByUphoneHmac(h);     // ← 레포지토리에 추가 예정
    }

    /* ===================== 가입 ===================== */

    @Transactional
    public String signup(UserDto dto) {
        // 1) 비밀번호 해시
        String hashed = StringUtils.hasText(dto.getUpw())
                ? BCrypt.hashpw(dto.getUpw(), BCrypt.gensalt(12))
                : null;

        // 2) 주민번호 정규화
        String rrnNorm;
        if (StringUtils.hasText(dto.getRrn())) {
            rrnNorm = userUtil.normalizeRrn(dto.getRrn());
        } else {
            rrnNorm = userUtil.normalizeRrnParts(dto.getRrnFront(), dto.getRrnBack());
        }

        // 3) 성별/생년월일 유도 (가능한 경우)
        String ugender = null;
        String ubirth  = null;
        if (userUtil.isValidRrn13(rrnNorm)) {
            ugender = deriveGenderFromRrn(rrnNorm); // 'M' / 'F'
            ubirth  = deriveBirthFromRrn(rrnNorm);  // YYYY-MM-DD
        }

        // 4) 주민번호 중복 체크(HMAC) → 레이스 대비 try-catch로 2차 방어
        String rrnEnc = null;
        String rrnHmac = null;
        if (userUtil.isValidRrn13(rrnNorm)) {
            rrnHmac = userUtil.hmacRrnHex(rrnNorm);
            if (userRepository.existsByUrrnHmac(rrnHmac)) {
                throw new IllegalArgumentException("이미 등록된 주민등록번호입니다.");
            }
            rrnEnc = userUtil.encryptRrn(rrnNorm); // AES-GCM (IV 포함)
        }

        // 5) 휴대폰 처리: 정규화 → 형식 검증 → HMAC 중복 체크 → AES 암호화
        String phoneEnc = null;
        String phoneHmac = null;
        if (StringUtils.hasText(dto.getUphone())) {
            String phoneNorm = userUtil.normalizePhone(dto.getUphone());
            if (!userUtil.isValidPhone(phoneNorm)) {
                throw new IllegalArgumentException("휴대폰 형식 오류(01로 시작, 10~11자리)");
            }
            phoneHmac = userUtil.hmacPhoneHex(phoneNorm);
            if (userRepository.existsByUphoneHmac(phoneHmac)) {
                throw new IllegalArgumentException("이미 등록된 휴대전화번호입니다.");
            }
            phoneEnc = userUtil.encryptPhone(phoneNorm); // "iv:ct"
        }

        // 6) 엔티티 매핑
        User u = new User();
        u.setUid(dto.getUid());
        u.setUpw(hashed);
        u.setUname(dto.getUname());
        u.setUgender(StringUtils.hasText(ugender) ? ugender : "M");
        u.setUbirth(ubirth);
        // ⚠️ 평문 uphone은 더 이상 저장하지 않는 것을 권장
        // u.setUphone(null); // 필요 시 제거/Transient 전환
        u.setUrole("ROLE_USER");
        u.setUcheck("N");
        u.setUshare(0L);

        // 목록 → CSV
        u.setUcurrency(dto.getUcurrency() != null && !dto.getUcurrency().isEmpty()
                ? String.join(",", dto.getUcurrency()) : null);
        u.setUinterest(dto.getUinterest() != null && !dto.getUinterest().isEmpty()
                ? String.join(",", dto.getUinterest()) : null);

        // AES/HMAC 저장
        u.setUrrnEnc(rrnEnc);
        u.setUrrnHmac(rrnHmac);
        u.setUphoneEnc(phoneEnc);   // ← 엔티티 필드 필요
        u.setUphoneHmac(phoneHmac); // ← 엔티티 필드 필요
        // ukeyVer는 엔티티 디폴트 'v1'을 사용한다면 그대로

        try {
            return userRepository.save(u).getUid();
        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약 위반(주민번호 또는 휴대폰 HMAC)
            // 스키마에 어떤 제약이 걸렸는지에 따라 메시지를 분기해도 됨
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
    
    /** 알림푸쉬 권한 관련*/
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