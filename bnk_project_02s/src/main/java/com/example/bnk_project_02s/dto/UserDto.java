package com.example.bnk_project_02s.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDto {

    /* ───── 기본 정보 ───── */
    @NotBlank @Size(min = 4, max = 50, message = "ID는 4~50자 사이여야 합니다")
    private String uid;

    /** 비밀번호: ① 4자 이상 ② 대문자·특수문자 포함 */
    @Size(min = 4, message = "비밀번호는 최소 4자 이상이어야 합니다")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$",
             message = "비밀번호는 대문자와 특수문자를 포함해야 합니다")
    private String upw;

    @NotBlank @Size(max = 30, message = "이름은 30자 이하로 입력하세요")
    private String uname;

    @Pattern(regexp = "^[MF]$", message = "성별은 M 또는 F 로 입력하세요")
    private String ugender;

    @Pattern(regexp = "^\\d{8}$", message = "생년월일은 YYYYMMDD 형식입니다")
    private String ubirth;

    @Pattern(regexp = "^01\\d-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
    private String uphone;

    /* ───── 권한 & 상태 ───── */
    private String urole;
    private String ucheck;
    private Long   ushare;

    /* ───── 다중 선택 필드 ───── */
    @NotEmpty(message = "관심 통화를 한 가지 이상 선택하세요")
    private List<String> ucurrency;

    @NotEmpty(message = "관심 분야를 한 가지 이상 선택하세요")
    private List<String> uinterest;
}