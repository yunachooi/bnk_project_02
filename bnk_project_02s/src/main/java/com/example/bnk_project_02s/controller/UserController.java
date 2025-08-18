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

import jakarta.servlet.http.HttpServletRequest;
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
    private static final String RETURN_TO  = "RETURN_TO";

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
        return "user/signup-step1";
    }

    @PostMapping("/signup/step1")
    public String step1Submit(@Valid @ModelAttribute("userDto") UserDto dto,
                              BindingResult binding,
                              HttpSession session) {

        if (binding.hasErrors()) return "user/signup-step1";

        if (userService.existsByUid(dto.getUid())) {
            binding.rejectValue("uid", "duplicate", "이미 사용 중인 아이디입니다.");
            return "user/signup-step1";
        }

        if (!userService.isValidRrn(dto.getRrnFront(), dto.getRrnBack())) {
            binding.rejectValue("rrnFront", "format", "주민등록번호 형식이 올바르지 않습니다.");
            binding.rejectValue("rrnBack", "format", "주민등록번호 형식이 올바르지 않습니다.");
            return "user/signup-step1";
        }
        if (userService.isRrnDuplicate(dto.getRrnFront(), dto.getRrnBack())) {
            binding.rejectValue("rrnBack", "duplicate", "이미 등록된 주민등록번호입니다.");
            return "user/signup-step1";
        }

        if (!userService.isValidPhone(dto.getUphone())) {
            binding.rejectValue("uphone", "format",
                "휴대폰 형식이 올바르지 않습니다. (01로 시작, 숫자 10~11자리)");
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
        return "user/success";
    }

    /* ───────── 로그인/로그아웃 ───────── */

    // ✅ 변경: redirect 파라미터를 받으면 세션 RETURN_TO로 저장
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "redirect", required = false) String redirect,
                            Model m,
                            HttpSession session) {
        if (!m.containsAttribute("userDto")) {
            m.addAttribute("userDto", new UserDto());
        }
        // redirect 파라미터가 유효하면 세션에 저장 (오픈 리다이렉트 방지)
        if (StringUtils.hasText(redirect) && isSafeInternalPath(redirect)) {
            session.setAttribute(RETURN_TO, redirect);
        }
        return "user/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("uid") String uid,
                        @RequestParam("upw") String upw,
                        HttpSession session,
                        HttpServletRequest request,
                        RedirectAttributes ra) {
        User user = userService.authenticate(uid, upw);
        if (user == null) {
            ra.addFlashAttribute("loginError", "아이디 또는 비밀번호가 올바르지 않습니다.");
            ra.addFlashAttribute("userDto", new UserDto());
            // 실패 시엔 세션 RETURN_TO를 건드리지 않음(다음 로그인에서 재사용)
            return "redirect:/user/login";
        }

        // 로그인 성공 → 세션 저장
        session.setAttribute(LOGIN_USER, user);

        // 1) 세션의 RETURN_TO(내부 경로)로 복귀 시도
        String dest = consumeSafeReturnTo(session);

        // 2) 없으면 역할별 기본 경로
        if (dest == null) {
            if ("ROLE_ADMIN".equals(user.getUrole())) {
                dest = "/admin/adminMain";   // 기존 프로젝트 흐름에 맞춤
            } else {
                dest = "/user/userhome";
            }
        }

        return "redirect:" + dest;
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) session.invalidate();
        return "redirect:/";
    }

    /* ───────── 사용자 기본 홈 ───────── */
    @GetMapping("/userhome")
    public String userhome() {
        return "user/userMain";
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

    /* ===== 내부 유틸 ===== */

    /** 세션에서 RETURN_TO를 가져오되, 내부 경로(/로 시작)만 허용. 유효하지 않으면 null */
    private String consumeSafeReturnTo(HttpSession session) {
        if (session == null) return null;
        Object obj = session.getAttribute(RETURN_TO);
        session.removeAttribute(RETURN_TO);
        if (!(obj instanceof String s) || !StringUtils.hasText(s)) return null;

        // 내부 경로만 허용 (예: /mypage/info?tab=1)
        if (!s.startsWith("/")) return null;

        // 로그인/회원가입 같은 경로는 목적지로 사용하지 않음
        if (s.startsWith("/user/login") || s.startsWith("/user/signup") || s.startsWith("/user/logout")) {
            return null;
        }
        return s;
    }
    /** 내부 경로만 허용 (/로 시작하고 //로 시작하지 않음) */
    private boolean isSafeInternalPath(String path) {
        return path.startsWith("/") && !path.startsWith("//");
    }
}