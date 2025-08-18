package com.example.bnk_project_02s.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.entity.BnkPush;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.BnkPushRepository;
import com.example.bnk_project_02s.repository.UserRepository;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationPushService {
	
	private final UserRepository userRepository;
	private final BnkPushRepository pushRepository;
	
	@Data
	public static class Summary{
		private long totalTargets;
		private int inserted;
		private int skipped;
	}
	
	@Transactional
	public Summary enqueeRecommendations(int page, int size) {
		Page<User> targets = userRepository.findRecommendationTargets(PageRequest.of(page, size));
		Summary s = new Summary();
		s.setTotalTargets(userRepository.countRecommendationTargets());
		
		for(User u : targets) {
			if(u.getUid() == null || u.getUid().isBlank()) {
				s.setSkipped(s.getSkipped() + 1);
				continue;
			}
			String currency = (u.getUcurrency() == null || u.getUcurrency().isBlank())
					? "인기 외화" : u.getUcurrency();
			
			BnkPush row = new BnkPush();
			row.setUid(u.getUid());
			row.setKind("PRODUCT_RECO");
			row.setTitle("맞춤 환전 상품 추천");
			row.setBody((u.getUname() == null ? "고객" : u.getUname()) + "님, " + currency + " 우대 혜택을 확인해보세요!");
			row.setDataJson("{\"deeplink\":\"app://products/recommend\"}");
			
			pushRepository.save(row);
			s.setInserted(s.getInserted() + 1);
		}
		return s;
	}

}
