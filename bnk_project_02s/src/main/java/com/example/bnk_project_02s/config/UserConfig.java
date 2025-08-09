package com.example.bnk_project_02s.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class UserConfig implements WebMvcConfigurer {

    private final UserInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry reg) {
        reg.addInterceptor(interceptor)
           .addPathPatterns("/user/**", "/mypage/**")
           .excludePathPatterns(
               // 비로그인 허용 경로
               "/user/login",
               "/user/signup",
               "/user/signup/**",
               "/user/check-uid",
               "/user/check-rrn",   // ★ 추가: 주민등록 중복확인

               // 정적/공통
               "/css/**", "/js/**", "/images/**",
               "/favicon.ico", "/error"
           )
           .order(1);
    }
}