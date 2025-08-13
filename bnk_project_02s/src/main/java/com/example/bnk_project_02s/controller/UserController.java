package com.example.bnk_project_02s.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bnk_project_02s.dto.UserDto;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private static final String LOGIN_USER = "LOGIN_USER";
    private static final String SIGNUP_DTO = "SIGNUP_DTO";

    private final UserService userService;

    /* ───────── 중복확인: ID ───────── */
    @GetMapping("/check-uid")
    @ResponseBody
    public String checkUid(@RequestParam("uid") String uid) {
        if (!StringUtils.hasText(uid)) return "ID를 입력하세요.";
        boolean exists = userService.existsByUid(uid);
        return exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.";
    }

    /* ───────── 중복확인: 주민등록번호 ───────── */
    @GetMapping("/check-rrn")
    @ResponseBody
    public String checkRrn(@RequestParam("front") String rrnFront,
                           @RequestParam("back")  String rrnBack) {
        if (!userService.isValidRrn(rrnFront, rrnBack)) {
            return "주민등록번호 형식이 올바르지 않습니다.";
        }
        boolean exists = userService.isRrnDuplicate(rrnFront, rrnBack);
        return exists ? "이미 등록된 주민등록번호입니다." : "사용 가능한 주민등록번호입니다.";
    }

    /* ───────── 중복확인: 휴대번호 ───────── */
    @GetMapping("/check-phone")
    @ResponseBody
    public String checkPhone(@RequestParam("phone") String phone) {
        if (!userService.isValidPhone(phone)) {
            return "휴대폰 형식이 올바르지 않습니다. (01로 시작, 숫자 10~11자리)";
        }
        boolean exists = userService.isPhoneDuplicate(phone);
        return exists ? "이미 등록된 휴대전화번호입니다." : "사용 가능한 휴대전화번호입니다.";
    }

    /* ───────── 멀티스텝: step1 ───────── */
    @GetMapping("/signup")
    public String signupRedirect() {
        return "redirect:/user/signup/step1";
    }

    @GetMapping("/signup/step1")
    public String step1Form(Model m, HttpSession session) {
        Object cached = session.getAttribute(SIGNUP_DTO);
        m.addAttribute("userDto", (cached instanceof UserDto d) ? d : new UserDto());
        return "user/signup-step1";   // ⬅️ templates/user/signup-step1.html
    }

    @PostMapping("/signup/step1")
    public String step1Submit(@Valid @ModelAttribute("userDto") UserDto dto,
                              BindingResult binding,
                              HttpSession session) {

        // 1) Bean Validation 에러 우선 처리 (@AssertTrue 포함)
        if (binding.hasErrors()) return "user/signup-step1";

        // 2) 아이디 중복
        if (userService.existsByUid(dto.getUid())) {
            binding.rejectValue("uid", "duplicate", "이미 사용 중인 아이디입니다.");
            return "user/signup-step1";
        }

        // 3) 주민등록번호 검증/중복
        if (!userService.isValidRrn(dto.getRrnFront(), dto.getRrnBack())) {
            binding.rejectValue("rrnFront", "format", "주민등록번호 형식이 올바르지 않습니다.");
            binding.rejectValue("rrnBack", "format", "주민등록번호 형식이 올바르지 않습니다.");
            return "user/signup-step1";
        }
        if (userService.isRrnDuplicate(dto.getRrnFront(), dto.getRrnBack())) {
            binding.rejectValue("rrnBack", "duplicate", "이미 등록된 주민등록번호입니다.");
            return "user/signup-step1";
        }

        // 4) 휴대번호 검증/중복
        if (!userService.isValidPhone(dto.getUphone())) {
            binding.rejectValue("uphone", "format",
                "휴대폰 형식이 올바르지 않습니다. (01로 시작, 숫자 10~11자리)");
            return "user/signup-step1";
        }
        if (userService.isPhoneDuplicate(dto.getUphone())) {
            binding.rejectValue("uphone", "duplicate", "이미 등록된 휴대전화번호입니다.");
            return "user/signup-step1";
        }

        // 5) 통과 → 세션 저장
        session.setAttribute(SIGNUP_DTO, dto);
        return "redirect:/user/signup/step2";
    }

    /* ───────── 멀티스텝: step2 ───────── */
    @GetMapping("/signup/step2")
    public String step2Form(Model m, HttpSession session) {
        UserDto dto = (UserDto) session.getAttribute(SIGNUP_DTO);
        if (dto == null) return "redirect:/user/signup/step1";
        m.addAttribute("userDto", dto);
        return "user/signup-step2";   // ⬅️ templates/user/signup-step2.html
    }

    @PostMapping("/signup/step2")
    public String step2Submit(@ModelAttribute("userDto") UserDto form, HttpSession session) {
        UserDto dto = (UserDto) session.getAttribute(SIGNUP_DTO);
        if (dto == null) return "redirect:/user/signup/step1";
        dto.setUcurrency(form.getUcurrency());
        session.setAttribute(SIGNUP_DTO, dto);
        return "redirect:/user/signup/step3";
    }

    @GetMapping("/signup/step2/skip")
    public String step2Skip(HttpSession session) {
        if (session.getAttribute(SIGNUP_DTO) == null) return "redirect:/user/signup/step1";
        return "redirect:/user/signup/step3";
    }

    /* ───────── 멀티스텝: step3 ───────── */
    @GetMapping("/signup/step3")
    public String step3Form(Model m, HttpSession session) {
        UserDto dto = (UserDto) session.getAttribute(SIGNUP_DTO);
        if (dto == null) return "redirect:/user/signup/step1";
        m.addAttribute("userDto", dto);
        return "user/signup-step3";   // ⬅️ templates/user/signup-step3.html
    }

    @PostMapping("/signup/step3")
    public String step3Submit(@ModelAttribute("userDto") UserDto form,
                              HttpSession session,
                              RedirectAttributes ra,
                              Model model) {
        UserDto dto = (UserDto) session.getAttribute(SIGNUP_DTO);
        if (dto == null) return "redirect:/user/signup/step1";

        dto.setUinterest(form.getUinterest());

        try {
            String uid = userService.signup(dto); // BCrypt + RRN AES/HMAC + Phone AES/HMAC 저장
            session.removeAttribute(SIGNUP_DTO);
            ra.addFlashAttribute("signupOk", true);
            ra.addFlashAttribute("uid", uid);
            return "redirect:/user/signup/success";
        } catch (IllegalArgumentException e) {
            model.addAttribute("userDto", dto);
            model.addAttribute("error", e.getMessage());
            return "user/signup-step3";
        } catch (Exception e) {
            model.addAttribute("userDto", dto);
            model.addAttribute("error", "가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
            return "user/signup-step3";
        }
    }

    @GetMapping("/signup/step3/skip")
    public String step3Skip(HttpSession session, RedirectAttributes ra, Model model) {
        UserDto dto = (UserDto) session.getAttribute(SIGNUP_DTO);
        if (dto == null) return "redirect:/user/signup/step1";
        try {
            String uid = userService.signup(dto);
            session.removeAttribute(SIGNUP_DTO);
            ra.addFlashAttribute("signupOk", true);
            ra.addFlashAttribute("uid", uid);
            return "redirect:/user/signup/success";
        } catch (Exception e) {
            model.addAttribute("userDto", dto);
            model.addAttribute("error", "가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
            return "user/signup-step3";
        }
    }

    /* ───────── 성공 페이지 ───────── */
    @GetMapping("/signup/success")
    public String signupSuccess() {
        return "user/success";        // ⬅️ templates/user/success.html
    }

    /* ───────── 로그인/로그아웃 ───────── */
    @GetMapping("/login")
    public String loginForm() {
        return "user/login";          // ⬅️ templates/user/login.html
    }

    @PostMapping("/login")
    public String login(@RequestParam("uid") String uid,
                        @RequestParam("upw") String upw,
                        HttpSession session,
                        RedirectAttributes ra) {
        User user = userService.authenticate(uid, upw);
        if (user == null) {
            ra.addFlashAttribute("loginError", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "redirect:/user/login";
        }
        session.setAttribute(LOGIN_USER, user);

        // 역할에 따른 뷰 분기 (템플릿 위치 기준)
        if ("ROLE_ADMIN".equals(user.getUrole())) {
            return "admin/adminMain"; // ⬅️ templates/admin/adminMain.html (있다면)
        }
        return "user/userMain";       // ⬅️ templates/user/userMain.html
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // (선택) 직접 진입용
    @GetMapping("/userMain")
    public String userMain() {
        return "user/userMain";       // ⬅️ templates/user/userMain.html
    }

    /* ───────── push 동의 ───────── */
    @Data
    static class PushConsentReq {
        private String uid;
        private boolean consent;
    }

    @PostMapping("/push-consent")
    @ResponseBody
    public ResponseEntity<String> pushConsent(
            @RequestParam("uid") String uid,
            @RequestParam("consent") boolean consent) {
        userService.updatePushConsent(uid, consent);
        return ResponseEntity.ok("OK");
    }
    
    
}