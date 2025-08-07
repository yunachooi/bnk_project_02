package com.example.bnk_project_02s.service;

import com.example.bnk_project_02s.dto.UserDto;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.UserRepository;
import com.example.bnk_project_02s.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;

    public void register(UserDto dto) {

        if (userRepo.existsByUid(dto.getUid()))
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");

        if (dto.getUpw() == null || dto.getUpw().isBlank())
            throw new IllegalArgumentException("비밀번호는 필수입니다.");

        User user = UserUtil.toEntity(dto, true);
        userRepo.save(user);                // 트랜잭션 종결 시 commit
    }

    @Transactional(readOnly = true)
    public User login(String uid, String rawPw) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

        if (!BCrypt.checkpw(rawPw, user.getUpw()))
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");

        return user;
    }

    public void updateProfile(UserDto dto) {
        User user = userRepo.findById(dto.getUid())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        BeanUtils.copyProperties(dto, user, "upw", "uid");
        if (dto.getUpw() != null && !dto.getUpw().isBlank())
            user.setUpw(BCrypt.hashpw(dto.getUpw(), BCrypt.gensalt(12)));
    }

    @Transactional(readOnly = true)
    public UserDto findOne(String uid) {
        return UserUtil.toDto(
                userRepo.findById(uid)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."))
        );
    }
    
    public boolean existsByUid(String uid) {
        return userRepo.existsByUid(uid);
    }
    
}