package com.example.bnk_project_02s.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileView get(String uid) {
        User u = userRepository.findById(uid)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + uid));

        // ⚠️ @Convert(AesGcmConverter.class)가 붙어 있으므로
        // 엔티티에서 getUrrnEnc()/getUphoneEnc()를 호출하면 평문으로 반환되는 구조입니다.
        String rrnPlain   = u.getUrrnEnc();                      // 평문처럼 읽힘(저장은 암호문)
        String phonePlain = (u.getUphone() != null && !u.getUphone().isBlank())
                ? u.getUphone()
                : (u.getUphoneEnc() != null ? u.getUphoneEnc() : null); // enc 필드도 컨버터로 평문처럼 읽힘

        return ProfileView.builder()
                .uid(u.getUid())
                .name(u.getUname())
                .phonePlain(phonePlain)                   // 정책상 내려도 된다면 사용
                .residentIdMasked(maskResident(rrnPlain)) // 주민번호는 반드시 마스킹만
                .build();
    }

    private String maskResident(String rrn) {
        if (rrn == null || rrn.isBlank()) return "";

        // 숫자만 추출
        String digits = rrn.replaceAll("\\D", "");

        // 13자리(하이픈 없이 저장된 경우)
        if (digits.length() == 13) {
            return digits.substring(0, 6) + "-" + digits.substring(6, 7) + "******";
        }

        // 이미 하이픈 포함 "######-#######" 인 경우
        if (rrn.matches("^\\d{6}-\\d{7}$")) {
            return rrn.replaceFirst("^(\\d{6})-(\\d).*", "$1-$2******");
        }

        // 그 외 입력은 최대한 안전하게 처리: 앞 6자리만 노출
        if (digits.length() >= 7) {
            return digits.substring(0, 6) + "-" + digits.substring(6, 7) + "******";
        }
        return "";
}
}