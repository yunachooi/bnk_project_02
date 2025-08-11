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

    private static final String[] CURRENCIES = {"KRW","USD","EUR","JPY","CNY","GBP","AUD","CAD","HKD","TWD"};
    private static final String[] INTERESTS  = {"환전","여행","투자","쇼핑","저축","해외송금","가계부","카드혜택","예적금"};
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

            // 연락처(평문) 컬럼이 존재 → 반드시 값 넣기
            u.setUphone(String.format("010-%04d-%04d", rnd.nextInt(10000), rnd.nextInt(10000)));

            // 관심 통화/분야
            u.setUcurrency(String.join(",", pickSome(CURRENCIES, randBetween(1, 3))));
            u.setUinterest(String.join(",", pickSome(INTERESTS,  randBetween(1, 2))));

            // 상품가입여부는 NULL 허용
            u.setUcheck(null);

            // 권한
            u.setUrole(rnd.nextInt(100) < 92 ? "ROLE_USER" : "ROLE_ADMIN");

            // 동의(Y/N)
            String ynPush = randYN(0.5);
            String ynLoc  = randYN(0.5);
            u.setUpush(ynPush);
            u.setUpushdate("Y".equals(ynPush) ? LocalDate.now().minusDays(rnd.nextInt(180)).toString() : null);
            u.setUlocation(ynLoc);

            // 공유 수
            u.setUshare((long) rnd.nextInt(10_000));

            /* ====== 여기 핵심: NOT NULL 컬럼에 더미값 채우기 ====== */
            // 스키마가 NOT NULL이면 빈 문자열도 제약 위반일 수 있으므로 랜덤 HEX로 채움
            // 길이는 컬럼 길이보다 충분히 짧게(성능/가독성 고려)
            u.setUphoneEnc  (randHex(64));  // 예: 64자리 HEX (<= VARCHAR(512))
            u.setUphoneHmac (randHex(64));  // 예: 64자리 HEX (<= VARCHAR(128))
            u.setUrrnEnc   (randHex(128)); // 예: 128자리 HEX (<= VARCHAR(512))
            u.setUrrnHmac  (randHex(64));  // 예: 64자리 HEX (<= VARCHAR(128))

            userRepository.save(u);
        }
    }

    /* helpers */
    private int randBetween(int a, int b) { return a + rnd.nextInt(b - a + 1); }
    private String randYN(double pYes){ return rnd.nextDouble() < pYes ? "Y" : "N"; }

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
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        if (sb.length() > len) sb.setLength(len);
        return sb.toString();
    }
}
