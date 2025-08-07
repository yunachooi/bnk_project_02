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
               "/user/signup",    // ← POST 포함, 세션검사 제외
               "/user/login",
               "/css/**","/js/**","/images/**");
    }
}