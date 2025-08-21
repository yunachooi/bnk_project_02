package com.example.bnk_project_02s.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bnk_project_02s.entity.CustomerRate;
import com.example.bnk_project_02s.entity.ParentAccount;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.ChildAccountRepository;
import com.example.bnk_project_02s.repository.CurrencyRepository;
import com.example.bnk_project_02s.repository.ParentAccountRepository;
import com.example.bnk_project_02s.service.ExchangeHistoryService;
import com.example.bnk_project_02s.service.EximRateService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/exchange")
public class ExchangeController {

    private static final String LOGIN_USER = "LOGIN_USER";

    private final EximRateService eximRateService;
    private final ParentAccountRepository parentRepo;
    private final ExchangeHistoryService exchangeHistoryService;
    private final ChildAccountRepository childRepo;
    private final CurrencyRepository currencyRepo;
    /** 1단계: 구매 실행 화면 */
    @GetMapping("/buy/{code}")
    public String buyCurrency(@PathVariable("code") String code,
            Model model,
            HttpSession session,
            RedirectAttributes ra) {
			User me = (User) session.getAttribute(LOGIN_USER);
			if (me == null) return "redirect:/user/login";
			
			final String alpha = normalizeCode(code); // code=USD, JPY(100) 등 → USD
			var currency = currencyRepo.findByCunameIgnoreCase(alpha)
			.orElseThrow(() -> new IllegalArgumentException("통화가 존재하지 않습니다: " + alpha));
			
			var parent = parentRepo.findByUser_Uid(me.getUid())
			.orElseThrow(() -> new IllegalStateException("부모계좌(외화통장)가 없습니다."));
			
			boolean hasChild = childRepo
			.findByParentAccount_PanoAndCurrency_Cuname(parent.getPano(), currency.getCuname())
			.isPresent();
			
			if (!hasChild) {
			// ★ 플래시로 에러 메시지 전달 후 상세 페이지로 되돌림
			ra.addFlashAttribute("error", "해당 통화(" + currency.getCuname() + ") 계좌가 없어 구매할 수 없습니다.");
			return "redirect:/forex/detail?currency=" + code;
			}

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
            Model model
    ) {
        // 로그인 체크
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return "redirect:/user/login";
        }
        String uid = loginUser.getUid();

        // 부모계좌 조회
        Optional<ParentAccount> paOpt = parentRepo.findFirstByUser_Uid(uid);
        String panoRaw    = paOpt.map(ParentAccount::getPano).orElse("미지정");
        String branchName = paOpt.map(ParentAccount::getPabank).orElse(null);

        // 모델 바인딩
        model.addAttribute("rateCode",  code);
        model.addAttribute("fxAmount",  fxAmount);
        model.addAttribute("krwAmount", krwAmount);
        model.addAttribute("finalRate", finalRate);
        model.addAttribute("rateDate",  rateDate);
        model.addAttribute("rateRound", rateRound);
        model.addAttribute("savedFee",  savedFee);

        // 서버 조회값
        model.addAttribute("pano", panoRaw);
        model.addAttribute("branchName", branchName);

        return "forexExchange2"; // 확인 페이지
    }

    /** 3단계: 구매 완료 (DB 저장 수행 → 완료 화면으로 리다이렉트) */
    @PostMapping("/submit")
    public String submit(
            HttpSession session,
            @RequestParam("code") String code,
            @RequestParam("fxAmount") String fxAmount,
            @RequestParam("krwAmount") String krwAmount,
            @RequestParam("finalRate") String finalRate,
            @RequestParam(value = "rateDate",  required = false) String rateDate,
            @RequestParam(value = "rateRound", required = false) String rateRound,
            @RequestParam(value = "savedFee",  required = false) String savedFee,
            RedirectAttributes ra
    ) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return "redirect:/user/login";
        }
        String uid = loginUser.getUid();

        // 부모계좌 조회 (저장에 사용)
        ParentAccount parent = parentRepo.findFirstByUser_Uid(uid)
                .orElseThrow(() -> new IllegalStateException("부모계좌가 없습니다."));

        // === DB 저장 ===
        var saved = exchangeHistoryService.savePurchaseHistory(
                parent.getPano(),   // 출금 계좌(부모계좌 번호)
                code,               // 통화 코드 (예: USD)
                fxAmount,           // 외화 금액 (예: 600.00)
                krwAmount,          // 원화 금액 (예: 835,335 원)
                uid                 // 사용자 ID
        );

        // 완료 화면에 보여줄 값들 Flash로 전달
        ra.addFlashAttribute("rateCode", code);
        ra.addFlashAttribute("fxAmount", fxAmount);
        ra.addFlashAttribute("krwAmount", krwAmount);
        ra.addFlashAttribute("finalRate", finalRate);
        ra.addFlashAttribute("rateDate", rateDate);
        ra.addFlashAttribute("rateRound", rateRound);
        ra.addFlashAttribute("savedFee", savedFee);
        ra.addFlashAttribute("pano", parent.getPano());
        ra.addFlashAttribute("savedHno", saved.getHno()); // 저장 성공 확인용

        return "redirect:/exchange/complete";
    }

    /** 완료 화면 (PRG 패턴의 GET 렌더) */
    @GetMapping("/complete")
    public String completeView() {
        return "forexExchange3"; // 완료 페이지 템플릿
    }
    private static String normalizeCode(String s) {
        if (s == null) return "";
        String t = s.split("\\(")[0];       // 괄호 이후 제거
        t = t.replaceAll("[^A-Za-z]", "");  // 영문자만
        return t.trim().toUpperCase();
    }
}
