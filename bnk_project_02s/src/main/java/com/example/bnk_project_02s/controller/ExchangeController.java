package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.entity.CustomerRate;
import com.example.bnk_project_02s.entity.ParentAccount;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.ParentAccountRepository;
import com.example.bnk_project_02s.service.EximRateService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/exchange")
public class ExchangeController {

    private static final String LOGIN_USER = "LOGIN_USER";

    private final EximRateService eximRateService;
    private final ParentAccountRepository parentRepo;

    /** 1단계: 구매 실행 화면 */
    @GetMapping("/buy/{code}")
    public String buyCurrency(@PathVariable("code") String code, Model model) {
        CustomerRate rate = eximRateService.fetchAndSaveSingleRate(code);
        if (rate == null) {
            model.addAttribute("error", "환율 정보를 불러올 수 없습니다.");
            return "error";
        }
        model.addAttribute("currencyCode", code);
        model.addAttribute("rate", rate);
        model.addAttribute("today", rate.getCdate());
        model.addAttribute("prefRate", rate.getCpref());
        model.addAttribute("fee", rate.getCfee());
        model.addAttribute("finalRate", rate.getCfinal());
        model.addAttribute("rateDate", rate.getCdate());
        model.addAttribute("rateRound", 1); // 필요 시 회차 반영
        return "forexExchange"; // 1단계
    }

    /** 2단계: 구매 확인 (DB에서 부모계좌 조회해 표시) */
    @PostMapping("/confirm")
    public String confirm(
            HttpSession session,
            @RequestParam("code") String code,
            @RequestParam("fxAmount") String fxAmount,     // 외화 금액(문자열 유지)
            @RequestParam("krwAmount") String krwAmount,   // 원화 금액(정수 문자열)
            @RequestParam("finalRate") String finalRate,   // 고객 적용 환율
            @RequestParam(value = "rateDate",  required = false) String rateDate,
            @RequestParam(value = "rateRound", required = false) String rateRound,
            @RequestParam(value = "savedFee",  required = false) String savedFee,
            @RequestParam(value = "account",   required = false) String ignoredAccountMasked,
            Model model
    ) {
        // ✅ 로그인 유저 세션에서 가져오기 (UserController와 동일 키 사용)
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            // 로그인 안 되어 있으면 로그인 페이지로
            return "redirect:/user/login";
        }
        String uid = loginUser.getUid();

     // ✅ 부모계좌 조회 → 마스킹/지점명
        var paOpt = parentRepo.findFirstByUser_Uid(uid);
        String panoRaw    = paOpt.map(ParentAccount::getPano).orElse("미지정");
        String branchName = paOpt.map(ParentAccount::getPabank).orElse(null);

        // ✅ 모델 바인딩 (뷰에서 그대로 출력)
        model.addAttribute("rateCode",  code);
        model.addAttribute("fxAmount",  fxAmount);
        model.addAttribute("krwAmount", krwAmount);
        model.addAttribute("finalRate", finalRate);
        model.addAttribute("rateDate",  rateDate);
        model.addAttribute("rateRound", rateRound);
        model.addAttribute("savedFee",  savedFee);

        // 핵심: 서버 조회값 사용
        model.addAttribute("pano", panoRaw);
        model.addAttribute("branchName", branchName);

        return "forexExchange2"; // 확인 페이지
    }


    /** 3단계: 구매 완료 화면 (임시 - DB 저장 없음) */
    @PostMapping("/submit")
    public String doneTemp(
            @RequestParam("code") String code,
            @RequestParam("fxAmount") String fxAmount,
            @RequestParam("krwAmount") String krwAmount,
            @RequestParam("finalRate") String finalRate,
            @RequestParam(value = "rateDate", required = false) String rateDate,
            @RequestParam(value = "rateRound", required = false) String rateRound,
            @RequestParam(value = "account", required = false) String accountMasked,
            @RequestParam(value = "savedFee", required = false) String savedFee,
            Model model
    ) {
        model.addAttribute("rateCode", code);
        model.addAttribute("fxAmount", fxAmount);
        model.addAttribute("krwAmount", krwAmount);
        model.addAttribute("finalRate", finalRate);
        model.addAttribute("rateDate", rateDate);
        model.addAttribute("rateRound", rateRound);
        model.addAttribute("accountMasked", accountMasked); // 2단계 hidden으로 전달됨
        model.addAttribute("savedFee", savedFee);
        return "forexExchange3"; // 임시 완료 페이지
    }
}
