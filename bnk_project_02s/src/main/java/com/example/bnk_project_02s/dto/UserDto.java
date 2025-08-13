package com.example.bnk_project_02s.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserDto {

    @NotBlank(message = "ID는 필수입니다.")
    @Size(min = 4, max = 20, message = "ID는 4~20자여야 합니다.")
    private String uid;

    @NotBlank(message = "비밀번호는 필수입니다.")
    // 대문자 1+ / 특수문자 1+ / 전체 8+
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]).{8,}$",
        message = "대문자/특수문자 포함 8자 이상으로 입력하세요."
    )
    private String upw;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String confirmUpw;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 30, message = "이름은 30자 이내여야 합니다.")
    private String uname;

    @NotBlank(message = "주민번호 앞 6자리는 필수입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "생년월일은 숫자 6자리여야 합니다.")
    private String rrnFront;

    @NotBlank(message = "주민번호 뒤 7자리는 필수입니다.")
    @Pattern(regexp = "^\\d{7}$", message = "뒷자리는 숫자 7자리여야 합니다.")
    private String rrnBack;

    // 선택값: 비어있거나 형식 일치
    @Pattern(
        regexp = "^$|^0\\d{1,2}-?\\d{3,4}-?\\d{4}$",
        message = "휴대번호 형식 오류(예: 010-1234-5678)"
    )
    private String uphone;

    private List<String> ucurrency = new ArrayList<>();
    private List<String> uinterest = new ArrayList<>();

    /** 비밀번호와 확인 일치 검증 (Bean Validation) */
    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        if (upw == null || confirmUpw == null) return false;
        return upw.equals(confirmUpw);
    }

    /** 서비스에서 합쳐 쓸 주민번호(하이픈 포함) */
    public String getRrn() {
        if (rrnFront == null || rrnBack == null) return null;
        return rrnFront + "-" + rrnBack;
    }
}