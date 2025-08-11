package com.example.bnk_project_02s;
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

    @Test
    void insertDummyUsers() {
        String[] currencies = {"KRW", "USD", "EUR", "JPY", "CNY"};
        String[] interests = {"환전", "여행", "투자", "쇼핑", "저축"};
        String[] genders = {"남성", "여성"};
        String[] roles = {"USER", "MANAGER", "ADMIN"};
        Random random = new Random();

        for (int i = 1; i <= 300; i++) {
            User user = new User();

            user.setUid("user" + i);

            // 생년월일 1990~1999 랜덤
            int year = 1990 + random.nextInt(10);
            int month = 1 + random.nextInt(12);
            int day = 1 + random.nextInt(28);
            user.setUbirth(String.format("%04d-%02d-%02d", year, month, day));

            // 체크 여부
            user.setUcheck((i % 2 == 0) ? "Y" : "N");

            // 환율 관심통화 랜덤 선택
            user.setUcurrency(currencies[random.nextInt(currencies.length)]);

            // 성별
            user.setUgender(genders[i % 2]);

            // 관심사
            user.setUinterest(interests[random.nextInt(interests.length)]);

            // 이름
            user.setUname("사용자" + i);

            // 전화번호
            user.setUphone("010-" + String.format("%04d-%04d", random.nextInt(10000), random.nextInt(10000)));

            // 비밀번호 (임시 값)
            user.setUpw("pw" + i);

            // 권한
            user.setUrole(roles[i % roles.length]);

            // 공유 클릭 수 (0 ~ 9999 랜덤)
            user.setUshare((long) (random.nextInt(20)));

            userRepository.save(user);
        }
    }
}
