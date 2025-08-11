package com.example.bnk_project_02s;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.bnk_project_02s.entity.Review;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.ReviewRepository;
import com.example.bnk_project_02s.repository.UserRepository;

@SpringBootTest
class ReviewInsertTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;

    private final Random rnd = new SecureRandom();

    @Test
    void insertDummyReviews() {
        // 유저 확보
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) throw new IllegalStateException("유저가 없습니다. 먼저 더미 유저를 생성하세요.");
        List<String> uids = users.stream().map(User::getUid).toList();

        // rvno 시작값: 기존 최대값 다음 번호부터
        int start = reviewRepository.findMaxRvno();

        // 100개 생성
        IntStream.rangeClosed(1, 100).forEach(i -> {
            String rvno   = String.valueOf(start + i);
            String uid    = uids.get(rnd.nextInt(uids.size()));
            double rating = randomRatingValue();          // 1.0~5.0 (0.5 step)

            Review r = Review.builder()
                    .rvno(rvno)
                    .uid(uid)
                    .rvcontent(makeRichKoreanReview(rating))
                    .rvrating(ratingToString(rating))
                    .rvdate(LocalDate.now().minusDays(rnd.nextInt(180)).toString()) // 최근 180일 랜덤
                    .build();

            reviewRepository.save(r);
        });
    }

    /* ===== 유틸 ===== */

    private double randomRatingValue() {
        int half = rnd.nextInt(9) + 2; // 2..10 -> 1.0..5.0
        return half * 0.5;
    }
    private String ratingToString(double v) {
        return (v == Math.floor(v)) ? Integer.toString((int) v) : Double.toString(v);
    }

    private String makeRichKoreanReview(double rating) {
        boolean positive = rating >= 4.0;
        boolean neutral  = rating >= 3.0 && rating < 4.0;

        String[] openers = {
            "여행 준비하면서", "업무로 해외 결제를 자주 쓰는데", "최근 업데이트 이후",
            "환율 급등락 시기에", "가족 해외여행 중에", "첫 사용인데",
            "장기간 사용해 보니", "친구 추천으로 설치했는데", "타 서비스와 비교해보면",
            "주말 동안 집중 사용해 보니"
        };
        String[] features = {
            "환율 알림 정확도", "해외송금 처리 속도", "수수료 수준", "앱 안정성",
            "UI/UX 편의성", "차트·그래프 가독성", "프로모션 혜택", "보안/인증 절차",
            "고객센터 응대", "다국어 지원", "카드/계좌 연동", "위젯/알림 설정",
            "검색 및 자동완성", "다크모드/접근성"
        };
        String[] pos = { "매우 만족스럽습니다", "생각보다 훨씬 좋았습니다", "매끄럽게 동작합니다",
                         "정확하고 빠릅니다", "신뢰가 갑니다", "자주 쓰게 됩니다", "추천합니다" };
        String[] neu = { "대체로 괜찮습니다", "필요한 기능은 대부분 갖췄습니다",
                         "간헐적으로 느려질 때가 있지만 사용할 만합니다", "점진적으로 개선되는 게 보입니다" };
        String[] neg = { "아쉬움이 남습니다", "자주 끊깁니다", "로딩이 길어 불편합니다",
                         "정확도가 떨어집니다", "개선이 시급합니다" };
        String[] wish = {
            "알림 기준을 더 세분화할 수 있으면 좋겠습니다",
            "송금 수수료가 조금 더 내려가면 좋겠습니다",
            "그래프 비교 기능이 추가되면 분석이 쉬울 것 같습니다",
            "오프라인에서도 일부 기능이 동작하면 좋겠습니다",
            "고객센터 채팅 대기 시간을 줄여주셨으면 합니다",
            "여러 통화 동시 모니터링을 지원하면 좋겠습니다",
            "위젯 크기와 테마를 더 다양하게 제공해 주세요"
        };

        String o = pick(openers);
        String f1 = pick(features);
        String f2 = pickDistinct(features, f1);
        String v1 = positive ? pick(pos) : (neutral ? pick(neu) : pick(neg));
        String v2 = positive ? pick(pos) : (neutral ? pick(neu) : pick(neg));
        String w  = pick(wish);

        List<String> s = new ArrayList<>();
        s.add(o + " " + f1 + "이(가) " + v1 + ".");
        s.add("또한 " + f2 + " 측면에서도 " + v2 + ".");
        if (rnd.nextBoolean()) s.add("향후에는 " + w + ".");
        return String.join(" ", s);
    }
    private String pick(String[] arr){ return arr[rnd.nextInt(arr.length)]; }
    private String pickDistinct(String[] arr, String not){
        String x; do { x = pick(arr); } while (x.equals(not)); return x;
    }
}
