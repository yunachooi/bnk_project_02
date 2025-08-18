package com.example.bnk_project_02s.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bnk_project_02s.entity.BnkPush;

public interface BnkPushRepository extends JpaRepository<BnkPush, Long>{
	
	 List<BnkPush> findTop20ByUidAndConsumedOrderByIdAsc(String uid, boolean consumed);

	    @Modifying
	    @Query("""
	        update BnkPush p
	           set p.consumed = true, p.consumedAt = CURRENT_TIMESTAMP
	         where p.uid = :uid and p.id in :ids and p.consumed = false
	    """)
	    int ack(@Param("uid") String uid, @Param("ids") List<Long> ids);
	}
