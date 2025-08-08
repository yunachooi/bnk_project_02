package com.example.bnk_project_02s.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class UserInterceptor implements HandlerInterceptor {

    private static final String LOGIN_USER = "LOGIN_USER";
    private static final String RETURN_TO  = "RETURN_TO";

    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res,
                             Object handler) throws Exception {

        // 1) 사전 허용: OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }

        // 2) 공용(비로그인 허용) 경로는 통과
        final String path = req.getRequestURI();
        if (isPublicPath(path)) {
            return true;
        }

        // 3) 로그인 여부 확인
        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute(LOGIN_USER) != null;
        if (loggedIn) {
            return true;
        }

        // 4) 미로그인 처리
        if (isAjax(req)) {
            // AJAX 요청은 401 JSON 반환
            sendUnauthorizedJson(res);
        } else {
            // 일반 요청은 로그인 후 원래 URL로 복귀
            String original = getFullURL(req);
            req.getSession(true).setAttribute(RETURN_TO, original);
            res.sendRedirect("/user/login");
        }
        return false;
    }

    /* ================= helpers ================= */

    private boolean isPublicPath(String path) {
        if (path == null) return false;
        return path.equals("/")
            || path.startsWith("/user/login")
            || path.startsWith("/user/signup")     // step1/2/3/success 포함
            || path.startsWith("/user/check-uid")
            || path.equals("/error")
            || path.equals("/favicon.ico")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/");
    }

    private boolean isAjax(HttpServletRequest req) {
        String xrw = req.getHeader("X-Requested-With");
        String accept = req.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(xrw)
            || (accept != null && accept.contains("application/json"));
    }

    private void sendUnauthorizedJson(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"redirect\":\"/user/login\"}");
    }

    private String getFullURL(HttpServletRequest req) {
        String qs = req.getQueryString();
        return req.getRequestURI() + (qs != null ? "?" + qs : "");
    }
}