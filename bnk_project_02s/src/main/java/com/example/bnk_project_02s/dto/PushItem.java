package com.example.bnk_project_02s.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 앱으로 내려주는 "알림 항목" 응답 DTO (클래스 버전).
 * - 불필요한 엔티티 노출을 막기 위해 컨트롤러 응답 전용으로 사용하세요.
 * - 기본 생성자 + 전체 필드 생성자 + getter/setter 포함.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushItem {

    private Long id;
    private String kind;
    private String title;
    private String body;
    private String dataJson;
    
}