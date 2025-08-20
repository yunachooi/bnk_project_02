// import (스프링부트 3.x)
// ↓ 2.x 쓰면 javax.servlet.http.HttpSession 으로 바꿔주세요.
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.dto.ForeignMainSummary;
import com.example.bnk_project_02s.service.ForeignMainService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/foreign")
public class ForeignMainController {

    private final ForeignMainService service;

    @GetMapping("/summary")
    public ResponseEntity<ForeignMainSummary> summary(
            @RequestParam(value = "uid", required = false) String uidParam, // 테스트용 옵션
            HttpSession session
    ) {
        // 1) 세션에서 uid 조회 (로그인 시 세팅해둔 값)
        String uid = (String) session.getAttribute("loginUid");

        // 2) 세션 없으면 쿼리파라미터(uid)로도 허용(개발/테스트 편의)
        if (uid == null) uid = uidParam;

        // 3) 로그인 상태 판단
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.ok(
                ForeignMainSummary.builder()
                    .isLoggedIn(false).hasProducts(false).accountData(null).build()
            );
        }

        // 4) 요약 반환 (부모계좌 번호 + 통화별 잔액)
        return ResponseEntity.ok(service.getSummary(uid));
    }
}
