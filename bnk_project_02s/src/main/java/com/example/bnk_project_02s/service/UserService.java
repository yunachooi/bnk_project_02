package com.example.bnk_project_02s.service;

import com.example.bnk_project_02s.dto.UserDto;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.UserRepository;
import com.example.bnk_project_02s.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserUtil userUtil;

    public boolean existsByUid(String uid) {
        return StringUtils.hasText(uid) && userRepository.existsByUid(uid);
    }

    @Transactional
    public String signup(UserDto dto) {
        // 비밀번호 해시
        String hashed = null;
        if (StringUtils.hasText(dto.getUpw())) {
            hashed = BCrypt.hashpw(dto.getUpw(), BCrypt.gensalt(12));
        }

        // 주민번호 합치기 (rrnFront + rrnBack)
        String rrn = dto.getRrn();

        // 성별/생년월일 유도
        String ugender = null;
        String ubirth  = null;
        if (StringUtils.hasText(rrn)) {
            ugender = deriveGenderFromRrn(rrn); // 'M'/'F'
            ubirth  = deriveBirthFromRrn(rrn);  // ✅ YYYY-MM-DD
        }

        // 주민번호 암호화
        String rrnEnc = null;
        if (StringUtils.hasText(rrn)) {
            rrnEnc = userUtil.encryptRrn(rrn);
        }

        // 전화번호 정규화
        String formattedPhone = normalizePhone(dto.getUphone());

        // 엔티티 매핑
        User u = new User();
        u.setUid(dto.getUid());
        u.setUpw(hashed);
        u.setUname(dto.getUname());
        u.setUgender(StringUtils.hasText(ugender) ? ugender : "M");
        u.setUbirth(ubirth);                           // 예: 1990-01-05
        u.setUphone(formattedPhone);

        // 기본값 고정(DTO에 해당 필드 없으므로)
        u.setUrole("ROLE_USER");
        u.setUcheck("N");
        u.setUshare(0L);

        // List → CSV (비어있으면 null)
        u.setUcurrency(dto.getUcurrency() != null && !dto.getUcurrency().isEmpty()
                ? String.join(",", dto.getUcurrency()) : null);
        u.setUinterest(dto.getUinterest() != null && !dto.getUinterest().isEmpty()
                ? String.join(",", dto.getUinterest()) : null);

        u.setRrnEnc(rrnEnc);

        return userRepository.save(u).getUid();
    }

    @Transactional(readOnly = true)
    public User authenticate(String uid, String rawPw) {
        if (!StringUtils.hasText(uid) || !StringUtils.hasText(rawPw)) return null;

        Optional<User> opt = userRepository.findByUid(uid);
        if (opt.isEmpty()) return null;

        User u = opt.get();
        if (!StringUtils.hasText(u.getUpw())) return null;

        return BCrypt.checkpw(rawPw, u.getUpw()) ? u : null;
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        String digits = phone.replaceAll("\\D", ""); // 숫자만
        if (digits.startsWith("010")) {
            if (digits.length() == 11) { // 010-####-####
                return digits.substring(0,3) + "-" + digits.substring(3,7) + "-" + digits.substring(7);
            } else if (digits.length() == 10) { // 010-####-###
                return digits.substring(0,3) + "-" + digits.substring(3,7) + "-" + digits.substring(7);
            }
        }
        // 이미 하이픈 포함 & 패턴 맞으면 그대로 두기
        if (phone.matches("^010-\\d{4}-\\d{3,4}$")) return phone;
        return phone; // 그 외는 입력 유지(원하면 여기서 null로 막아도 됨)
    }

    private String deriveGenderFromRrn(String rrn) {
        String digits = rrn.replaceAll("\\D", "");
        if (digits.length() != 13) return null;
        char g = digits.charAt(6);
        return switch (g) {
            case '1','3','5','7' -> "M";
            case '2','4','6','8' -> "F";
            default -> null;
        };
    }

    // ✅ 생년월일을 "YYYY-MM-DD" 포맷으로 반환
    private String deriveBirthFromRrn(String rrn) {
        String digits = rrn.replaceAll("\\D", "");
        if (digits.length() != 13) return null;
        String yy = digits.substring(0, 2);
        String mm = digits.substring(2, 4);
        String dd = digits.substring(4, 6);
        char g = digits.charAt(6);

        String century;
        if (g == '1' || g == '2') century = "19";
        else if (g == '3' || g == '4') century = "20";
        else return null;

        return century + yy + "-" + mm + "-" + dd; // 예: 1990-01-05
    }
}