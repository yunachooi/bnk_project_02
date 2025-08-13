package com.example.bnk_project_02s.auth;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HmacOf {
    String to();                 // 결과 필드명 (예: "phoneHmac")
    String domain() default "";  // 도메인 프리픽스 (예: "phone:")
}