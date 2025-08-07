package com.example.bnk_project_02s.config;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res,
                             Object handler) throws Exception {

        // 로그인 상태?
        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute("LOGIN_USER") != null;

        if (!loggedIn) {
            res.sendRedirect("/user/login");
            return false;
        }
        return true;
    }
}