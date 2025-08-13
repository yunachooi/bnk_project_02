package com.example.bnk_project_02s;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.UserRepository;

@SpringBootTest
class UserInsertTest {

    @Autowired
    private UserRepository userRepository;

    /** 고정 집합 */
    private static final String[] CURRENCIES = {"USD","JPY","CNH","EUR","CHF","VND"};
    private static final String[] INTERESTS  = {"TRAVEL","STUDY","SHOPPING","FINANCE","ETC"};
    private static final char[]   GENDERS    = {'M','F'};
    private static final String[] ROLES      = {"ROLE_USER","ROLE_ADMIN"};

    private final Random rnd = new Random();
    private final SecureRandom secRnd = new SecureRandom();

    @Test
    void insertDummyUsers() {
        // PK 중복 방지
        userRepository.deleteAllInBatch();

        for (int i = 1; i <= 300; i++) {
            User u = new User();
            u.setUid("user" + i);

            // 기본 정보
            u.setUname("사용자" + i);
            u.setUgender(String.valueOf(GENDERS[rnd.nextInt(GENDERS.length)]));
            u.setUpw("pw" + i);

            // 생년월일: 10~70대+ 순환
            int bucket = (i - 1) % 7;
            int[] ageRange = switch (bucket) {
                case 0 -> new int[]{10,19};
                case 1 -> new int[]{20,29};
                case 2 -> new int[]{30,39};
                case 3 -> new int[]{40,49};
                case 4 -> new int[]{50,59};
                case 5 -> new int[]{60,69};
                default -> new int[]{70,85};
            };
            int curYear = LocalDate.now().getYear();
            int age   = randBetween(ageRange[0], ageRange[1]);
            int year  = curYear - age;
            int month = randBetween(1, 12);
            int day   = randBetween(1, YearMonth.of(year, month).lengthOfMonth());
            u.setUbirth(String.format("%04d-%02d-%02d", year, month, day));

            // 연락처(평문) — 스키마에 따라 존재한다면 반드시 값 넣기
            u.setUphone(String.format("010-%04d-%04d", rnd.nextInt(10000), rnd.nextInt(10000)));

            // ✅ 관심 통화 / 관심 분야: 여러 개 가능 (쉼표로 저장)
            int currencyCount = randBetween(1, Math.min(3, CURRENCIES.length)); // 1~3개
            int interestCount = randBetween(1, Math.min(3, INTERESTS.length));  // 1~3개
            u.setUcurrency(String.join(",", pickSome(CURRENCIES, currencyCount)));
            u.setUinterest(String.join(",", pickSome(INTERESTS,  interestCount)));

            // 상품가입여부는 NULL 허용
            u.setUcheck(null);

            // 권한
            u.setUrole(rnd.nextInt(100) < 92 ? ROLES[0] : ROLES[1]);

            // 동의(Y/N)
            String ynPush = randYN(0.5);
            String ynLoc  = randYN(0.5);
            u.setUpush(ynPush);
            u.setUpushdate("Y".equals(ynPush) ? LocalDate.now().minusDays(rnd.nextInt(180)).toString() : null);
            u.setUlocation(ynLoc);

            // 공유 수
            u.setUshare((long) rnd.nextInt(10_000));

            /* ====== NOT NULL 컬럼에 더미값 채우기 (스키마 제약 고려) ====== */
            u.setUphoneEnc (randHex(64));
            u.setUphoneHmac(randHex(64));
            u.setUrrnEnc   (randHex(128));
            u.setUrrnHmac  (randHex(64));

            userRepository.save(u);
        }
    }

    /* helpers */
    private int randBetween(int a, int b) { return a + rnd.nextInt(b - a + 1); }
    private String randYN(double pYes){ return rnd.nextDouble() < pYes ? "Y" : "N"; }

    /** 배열에서 k개만큼 랜덤으로 뽑아(중복 없이) 반환 */
    private List<String> pickSome(String[] src, int k) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, src);
        Collections.shuffle(list, rnd);
        return list.subList(0, Math.min(k, list.size()));
    }

    // n자리 HEX 문자열 생성
    private String randHex(int len){
        byte[] bytes = new byte[(len + 1) / 2];
        secRnd.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(len);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        if (sb.length() > len) sb.setLength(len);
        return sb.toString();
    }
}
