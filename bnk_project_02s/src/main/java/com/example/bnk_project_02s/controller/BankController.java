package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.dto.BankMarkerDto;
import com.example.bnk_project_02s.entity.Bank;
import com.example.bnk_project_02s.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankRepository repo;

    /** /branches -> bankSearch.html (기본 탭: 전체) */
    @GetMapping("/branches")
    public String bankSearch(
            @RequestParam(value = "tab", required = false) String tab,
            Model model
    ) {
        String initial = "digital".equalsIgnoreCase(tab) ? "digital" : "all";
        model.addAttribute("initialTab", initial);
        return "bankSearch"; // src/main/resources/templates/bankSearch.html
    }

    /**
     * /api/branches -> 지도용 지점 목록(JSON)
     * - 좌표가 채워진 지점만
     * - ?digital=true 이면 디지털(Y)만
     */
    @GetMapping("/api/branches")
    @ResponseBody
    public List<BankMarkerDto> listForMap(
            @RequestParam(value = "digital", required = false, defaultValue = "false") boolean digitalOnly
    ) {
        List<Bank> src = digitalOnly
                ? repo.findAllWithCoordsDigitalOnly()
                : repo.findAllWithCoords();

        return src.stream()
                // 혹시 공백 좌표가 섞여 있으면 방어
                .filter(b -> hasText(b.getBlatitude()) && hasText(b.getBlongitude()))
                .map(this::toDtoSafe)
                .filter(d -> !Double.isNaN(d.getLat()) && !Double.isNaN(d.getLng()))
                .toList();
    }

    /* ---------- 내부 유틸 ---------- */

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static double safeParse(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return Double.NaN; }
    }

    private BankMarkerDto toDtoSafe(Bank b) {
        double lat = safeParse(b.getBlatitude());
        double lng = safeParse(b.getBlongitude());
        boolean digital = "Y".equalsIgnoreCase(b.getBdigital());
        return new BankMarkerDto(
                b.getBno(),
                b.getBname(),
                b.getBaddress(),
                lat,
                lng,
                b.getBphone(),
                digital
        );
    }
}
