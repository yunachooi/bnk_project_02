package com.example.bnk_project_02s.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.History;
import com.example.bnk_project_02s.entity.ParentAccount;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    // ParentAccount 엔티티 자체로 조회
    List<History> findTop50ByParentAccountOrderByHdateDesc(ParentAccount parentAccount);

    // parentAccount.pano(문자열)로 조회
    List<History> findTop20ByParentAccount_PanoOrderByHdateDesc(String pano);

    // user.uid(문자열)로 조회
    List<History> findTop20ByUser_UidOrderByHdateDesc(String uid);
}
