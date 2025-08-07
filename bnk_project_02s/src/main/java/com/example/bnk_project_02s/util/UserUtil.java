package com.example.bnk_project_02s.util;

import com.example.bnk_project_02s.dto.UserDto;
import com.example.bnk_project_02s.entity.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserUtil {

    /* DTO ➜ Entity */
    public static User toEntity(UserDto dto, boolean encodePw) {
        User u = new User();

        u.setUid(dto.getUid());
        u.setUname(dto.getUname());
        u.setUgender(dto.getUgender());
        u.setUbirth(dto.getUbirth());
        u.setUphone(dto.getUphone());

        /* 다중 리스트 → 콤마 문자열 */
        u.setUcurrency(listToStr(dto.getUcurrency()));
        u.setUinterest(listToStr(dto.getUinterest()));

        u.setUrole(dto.getUrole() == null ? "ROLE_USER" : dto.getUrole());
        u.setUcheck(dto.getUcheck() == null ? "N" : dto.getUcheck());
        u.setUshare(dto.getUshare() == null ? 0L : dto.getUshare());

        if (encodePw && dto.getUpw() != null && !dto.getUpw().isBlank())
            u.setUpw(BCrypt.hashpw(dto.getUpw(), BCrypt.gensalt(12)));

        return u;
    }

    /* Entity ➜ DTO */
    public static UserDto toDto(User e) {
        return UserDto.builder()
                .uid(e.getUid())
                .uname(e.getUname())
                .ugender(e.getUgender())
                .ubirth(e.getUbirth())
                .uphone(e.getUphone())
                .urole(e.getUrole())
                .ucheck(e.getUcheck())
                .ushare(e.getUshare())
                .ucurrency(strToList(e.getUcurrency()))
                .uinterest(strToList(e.getUinterest()))
                .build();
    }

    /* 헬퍼 */
    private static String listToStr(List<String> list) {
        return (list == null || list.isEmpty()) ? null : String.join(",", list);
    }
    private static List<String> strToList(String str) {
        return (str == null || str.isBlank()) ? null : List.of(str.split(","));
    }
}