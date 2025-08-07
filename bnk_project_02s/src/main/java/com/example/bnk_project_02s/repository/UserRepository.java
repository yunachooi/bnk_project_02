package com.example.bnk_project_02s.repository;

import com.example.bnk_project_02s.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * User 테이블 전용 JPA 리포지터리
 *  - 기본 CRUD (save, findById, delete 등) 는 JpaRepository 가 제공
 *  - 커스텀 쿼리 메서드 existsByUid 로 중복 ID 체크
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /** 아이디 중복 여부 */
    boolean existsByUid(String uid);
}