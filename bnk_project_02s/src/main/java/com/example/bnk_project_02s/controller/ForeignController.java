package com.example.bnk_project_02s.controller;

import java.util.List;
import java.util.Optional;

import com.example.bnk_project_02s.dto.CardDto;
import com.example.bnk_project_02s.entity.ChildAccount;
import com.example.bnk_project_02s.entity.ParentAccount;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.ChildAccountRepository;
import com.example.bnk_project_02s.repository.HistoryRepository;
import com.example.bnk_project_02s.repository.ParentAccountRepository;
import com.example.bnk_project_02s.service.CardService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ForeignController {

    private static final String LOGIN_USER = "LOGIN_USER";

    private final ParentAccountRepository parentRepo;
    private final ChildAccountRepository childRepo;
    private final HistoryRepository historyRepo;
    private final CardService cardService;


    @GetMapping("/foreign")
    public String foreignPage(Model model, HttpSession session) {

        User login = (User) session.getAttribute(LOGIN_USER);
        boolean loggedIn = (login != null);

        model.addAttribute("loggedIn", loggedIn);

        // 공통: 쇼핑/리뷰 섹션에서 쓸 간단 유저명
        if (loggedIn) {
            model.addAttribute("uname", login.getUname());
            model.addAttribute("uid", login.getUid());
        }

        // ① 미로그인
        if (!loggedIn) {
            model.addAttribute("joined", false);
            model.addAttribute("canPay", false);
            model.addAttribute("canFx", false);
            model.addAttribute("banner", "로그인 후 외화통장을 이용하실 수 있어요.");
            // 구경 전용(후기/해외직구 목록은 템플릿에서 API로 로드)
            return "account/foreign";
        }

        // ② 로그인했지만 상품 미가입
        boolean joined = "Y".equalsIgnoreCase(String.valueOf(login.getUcheck()));
        model.addAttribute("joined", joined);

        if (!joined) {
            model.addAttribute("canPay", false);
            model.addAttribute("canFx", false);
            model.addAttribute("banner", "외화통장을 먼저 가입해 주세요.");
            return "account/foreign";
        }

        // ③ 로그인 + 가입완료 → 계좌/카드/거래내역 로드
        Optional<ParentAccount> paOpt = parentRepo.findByUser_Uid(login.getUid());
        if (paOpt.isPresent()) {
            ParentAccount pa = paOpt.get();
            model.addAttribute("pano", pa.getPano());
            model.addAttribute("pabank", pa.getPabank());

            List<ChildAccount> children = childRepo.findByParentAccount_Pano(pa.getPano());
            model.addAttribute("children", children);

            model.addAttribute("histories",
            	    historyRepo.findTop20ByParentAccount_PanoOrderByHdateDesc(pa.getPano()));
        } else {
            // 안전: 가입 Y인데도 모계좌 없을 때
            model.addAttribute("pano", null);
            model.addAttribute("children", List.of());
            model.addAttribute("histories", List.of());
        }

        // 카드(있으면 표시)
        try {
            CardDto card = cardService.getCardByUserId(login.getUid());
            model.addAttribute("card", card);
        } catch (Exception ignore) {
            model.addAttribute("card", null);
        }

        model.addAttribute("canPay", true);
        model.addAttribute("canFx", true);
        model.addAttribute("banner", null);

        return "account/foreign";
    }
}