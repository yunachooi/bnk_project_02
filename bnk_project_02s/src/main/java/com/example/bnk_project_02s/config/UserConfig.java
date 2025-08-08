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
               "/user/signup",        // 단일 엔드포인트
               "/user/signup/**",     // 다단계(step1/2/3/success 등)
               "/user/check-uid",

               // 정적/공통
               "/css/**", "/js/**", "/images/**",
               "/favicon.ico", "/error"
           );
    }
}