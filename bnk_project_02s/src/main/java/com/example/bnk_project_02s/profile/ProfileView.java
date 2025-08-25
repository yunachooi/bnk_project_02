package com.example.bnk_project_02s.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileView {
    private String uid;
    private String name;
    private String phonePlain;
    private String residentIdMasked;
    private String bankName;
    private String accountMasked;   // <- 원문 대신 마스킹만
}