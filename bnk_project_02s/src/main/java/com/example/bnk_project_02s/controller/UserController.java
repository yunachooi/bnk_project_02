package com.example.bnk_project_02s.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
    private static final String RETURN_TO  = "RETURN_TO";
    private static final String SIGNUP_DTO = "SIGNUP_DTO";

    private final UserService userService;

    /* ───────── 중복확인: ID ───────── */
    @GetMapping("/check-uid")
    @ResponseBody
    public String checkUid(@RequestParam("uid") String uid) {
        boolean exists = userService.existsByUid(uid);
        return exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.";
    }

    /* ───────── 중복확인: 주민등록번호(RRN) ───────── */
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

    /* ───────── 중복확인: 휴대번호(Phone, HMAC) ───────── */
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
    public String signupRedirect() { return "redirect:/user/signup/step1"; }

    @GetMapping("/signup/step1")
    public String step1Form(Model m, HttpSession session) {
        Object cached = session.getAttribute(SIGNUP_DTO);
        m.addAttribute("userDto", (cached instanceof UserDto d) ? d : new UserDto());
        return "user/signup-step1";
    }

    @PostMapping("/signup/step1")
    public String step1Submit(@Valid @ModelAttribute("userDto") UserDto dto,
                              BindingResult binding,
                              HttpSession session) {
        if (binding.hasErrors()) return "user/signup-step1";

        // 비밀번호 일치
        if (!dto.isPwMatched()) {
            binding.rejectValue("confirmUpw", "mismatch", "비밀번호가 일치하지 않습니다.");
            return "user/signup-step1";
        }
        // 아이디 중복
        if (userService.existsByUid(dto.getUid())) {
            binding.rejectValue("uid", "duplicate", "이미 사용 중인 아이디입니다.");
            return "user/signup-step1";
        }
        // ★ 주민등록번호 검증/중복
        if (!userService.isValidRrn(dto.getRrnFront(), dto.getRrnBack())) {
            binding.rejectValue("rrnFront", "format", "주민등록번호 형식이 올바르지 않습니다.");
            return "user/signup-step1";
        }
        if (userService.isRrnDuplicate(dto.getRrnFront(), dto.getRrnBack())) {
            binding.rejectValue("rrnFront", "duplicate", "이미 등록된 주민등록번호입니다.");
            return "user/signup-step1";
        }
        // ★ 휴대번호 검증/중복 (AES/HMAC 저장은 서비스에서 처리)
        if (!userService.isValidPhone(dto.getUphone())) {
            binding.rejectValue("uphone", "format", "휴대폰 형식이 올바르지 않습니다. (01로 시작, 숫자 10~11자리)");
            return "user/signup-step1";
        }
        if (userService.isPhoneDuplicate(dto.getUphone())) {
            binding.rejectValue("uphone", "duplicate", "이미 등록된 휴대전화번호입니다.");
            return "user/signup-step1";
        }

        session.setAttribute(SIGNUP_DTO, dto);
        return "redirect:/user/signup/step2";
    }

    /* ───────── 멀티스텝: step2 ───────── */
    @GetMapping("/signup/step2")
    public String step2Form(Model m, HttpSession session) {
        UserDto dto = (UserDto) session.getAttribute(SIGNUP_DTO);
        if (dto == null) return "redirect:/user/signup/step1";
        m.addAttribute("userDto", dto);
        return "user/signup-step2";
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
        return "user/signup-step3";
    }

    @PostMapping("/signup/step3")
    public String step3Submit(@ModelAttribute("userDto") UserDto form,
                              HttpSession session,
                              RedirectAttributes ra) {
        UserDto dto = (UserDto) session.getAttribute(SIGNUP_DTO);
        if (dto == null) return "redirect:/user/signup/step1";

        dto.setUinterest(form.getUinterest());
        String uid = userService.signup(dto); // 서비스에서 BCrypt + RRN AES/HMAC + Phone AES/HMAC 저장
        session.removeAttribute(SIGNUP_DTO);
        ra.addFlashAttribute("signupOk", true);
        ra.addFlashAttribute("uid", uid);
        return "redirect:/user/signup/success";
    }

    @GetMapping("/signup/step3/skip")
    public String step3Skip(HttpSession session, RedirectAttributes ra) {
        UserDto dto = (UserDto) session.getAttribute(SIGNUP_DTO);
        if (dto == null) return "redirect:/user/signup/step1";
        String uid = userService.signup(dto);
        session.removeAttribute(SIGNUP_DTO);
        ra.addFlashAttribute("signupOk", true);
        ra.addFlashAttribute("uid", uid);
        return "redirect:/user/signup/success";
    }

    /* ───────── 성공 페이지 ───────── */
    @GetMapping("/signup/success")
    public String signupSuccess() { return "user/success"; }

    /* ───────── 로그인/로그아웃 ───────── */
    @GetMapping("/login")
    public String loginForm() { return "user/login"; }

    // ROLE_ADMIN -> admin/adminMain, 그 외 -> user/userMain
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
        if ("ROLE_ADMIN".equals(user.getUrole())) {
            return "admin/adminMain";
        } else {
            return "user/userMain";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // 필요하면 GET 진입용 라우트도 추가 가능 (선택)
    @GetMapping("/userMain")
    public String userMain() { return "user/userMain"; }

    @GetMapping("/userhome")
    public String userhome() { return "user/userhome"; }
    
    //push 동의알림 관련
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