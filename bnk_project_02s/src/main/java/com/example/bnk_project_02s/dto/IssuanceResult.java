package com.example.bnk_project_02s.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class IssuanceResult {

    private String pano;
    private List<String> canos;
    private String cardNoMasked;
    private String cardStatus;
}
