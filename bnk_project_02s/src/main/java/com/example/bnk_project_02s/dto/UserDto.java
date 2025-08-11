package com.example.bnk_project_02s.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserDto {

    @NotBlank(message = "ID는 필수입니다.")
    private String uid;

    @NotBlank(message = "비밀번호는 필수입니다.")
    // 최소 8자, 대문자 1개 이상, 특수문자 1개 이상
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]).{8,}$",
             message = "대문자/특수문자 포함 8자 이상으로 입력하세요.")
    private String upw;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String confirmUpw;

    @NotBlank(message = "이름은 필수입니다.")
    private String uname;

    @NotBlank(message = "주민번호 앞 6자리는 필수입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "생년월일은 숫자 6자리여야 합니다.")
    private String rrnFront;

    @NotBlank(message = "주민번호 뒤 7자리는 필수입니다.")
    @Pattern(regexp = "^\\d{7}$", message = "뒷자리는 숫자 7자리여야 합니다.")
    private String rrnBack;

    @NotBlank(message = "휴대전화는 필수입니다.")
    @Pattern(
      regexp = "^010-\\d{4}-\\d{3,4}$",
      message = "휴대번호는 010-1111-1111 또는 010-1111-111 형식이어야 합니다."
    )
    private String uphone;

    private List<String> ucurrency = new ArrayList<>();
    private List<String> uinterest = new ArrayList<>();

    public boolean isPwMatched() {
        return upw != null && upw.equals(confirmUpw);
    }

    // ★ 서비스에서 쓰는 합쳐진 주민번호 제공
    public String getRrn() {
        if (rrnFront == null || rrnBack == null) return null;
        return rrnFront + "-" + rrnBack;
    }
}