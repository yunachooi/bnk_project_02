package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.dto.UserDto;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    /* ───────── 중복확인 ───────── */
    @GetMapping("/check-uid")
    @ResponseBody
    public String checkUid(@RequestParam("uid") String uid) {
        boolean exists = userService.existsByUid(uid);
        return exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.";
    }

    /* ───────── 회원가입 ───────── */

    @GetMapping("/signup")
    public String signupForm(Model m) {
        m.addAttribute("userDto", new UserDto());
        return "user/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("userDto") UserDto userDto,
                         BindingResult br,
                         RedirectAttributes ra) {

        if (br.hasErrors()) {
            return "user/signup";
        }
        userService.register(userDto);                // ← 예외 시 catch 불필요
        ra.addFlashAttribute("msg", "회원가입 완료! 로그인해 주세요.");
        return "redirect:/user/login";
    }

    /* ───────── 로그인 ───────── */

    @GetMapping("/login")
    public String loginForm() { return "user/login"; }

    @PostMapping("/login")
    public String login(@RequestParam(name = "uid") String uid,
                        @RequestParam(name = "upw") String upw,
                        HttpSession session,
                        RedirectAttributes ra) {
        try {
            User user = userService.login(uid, upw);
            session.setAttribute("LOGIN_USER", user);
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("loginError", e.getMessage());
            return "redirect:/user/login";
        }
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied"; // 이 위치에 HTML 필요
    }

    /* ───────── 로그아웃 ───────── */

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login";
    }

}