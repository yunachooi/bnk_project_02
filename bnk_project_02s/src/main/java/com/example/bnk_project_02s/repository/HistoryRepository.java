package com.example.bnk_project_02s.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.bnk_project_02s.entity.History;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findTop50ByPaaccountOrderByHdateDesc(String paaccount);
    List<History> findTop20ByUidOrderByHdateDesc(String uid);
}