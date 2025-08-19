package com.example.bnk_project_02s.service;

import com.example.bnk_project_02s.dto.*;
import com.example.bnk_project_02s.entity.*;
import com.example.bnk_project_02s.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ForeignFacadeService {
    private final ParentAccountRepository parentRepo;
    private final ChildAccountRepository childRepo;
    private final HistoryRepository historyRepo;
    private final CardService cardService;

    @Transactional(readOnly = true)
    public Optional<Snapshot> snapshot(String uid){
        // 리포지토리 시그니처에 맞게 하나 선택:
        return parentRepo.findFirstByUser_Uid(uid)   // 또는 findByUser_Uid(uid)
            .map(pa -> {
                List<ChildAccountDto> children = childRepo.findByParentAccount_Pano(pa.getPano())
                        .stream()
                        .map(c -> ChildAccountDto.builder()
                                .cano(c.getCano())
                                .pano(pa.getPano())
                                .cuno(c.getCurrency()!=null? c.getCurrency().getCuno() : null)
                                .cajoin(c.getCajoin())
                                .cabalance(c.getCabalance())
                                .pabank(c.getPabank())
                                .build())
                        .toList();

                // uid 기준 최근 20건 (원하면 pa 기준 메서드로 교체)
                List<History> histories = historyRepo.findTop20ByUidOrderByHdateDesc(uid);

                CardDto card = null;
                try { card = cardService.getCardByUserId(uid); } catch(Exception ignore){}

                return new Snapshot(
                        ParentAccountDto.builder()
                                .pano(pa.getPano())
                                .uid(uid)
                                .fno(pa.getFno())
                                .pajoin(pa.getPajoin())
                                .pabank(pa.getPabank())
                                .build(),
                        children, histories, card
                );
            });
    }

    public record Snapshot(ParentAccountDto parent,
                           List<ChildAccountDto> children,
                           List<History> histories,
                           CardDto card){}
}
