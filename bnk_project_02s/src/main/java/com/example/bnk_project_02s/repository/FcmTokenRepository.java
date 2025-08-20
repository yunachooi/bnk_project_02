package com.example.bnk_project_02s.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.bnk_project_02s.entity.FcmToken;
import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    @Query("SELECT f FROM FcmToken f WHERE f.user.uid = :uid AND f.isActive = 'Y'")
    List<FcmToken> findActiveTokensByUserId(@Param("uid") String uid);
    
    Optional<FcmToken> findByUserUidAndDeviceId(String uid, String deviceId);
    
    Optional<FcmToken> findByFcmToken(String fcmToken);
}