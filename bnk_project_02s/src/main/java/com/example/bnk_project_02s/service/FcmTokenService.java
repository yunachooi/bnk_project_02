package com.example.bnk_project_02s.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bnk_project_02s.entity.FcmToken;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.FcmTokenRepository;
import com.example.bnk_project_02s.repository.UserRepository;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FcmTokenService {
    
    @Autowired
    private FcmTokenRepository fcmTokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public boolean saveOrUpdateToken(String uid, String fcmToken, String deviceType, String deviceId) {
        Optional<User> userOpt = userRepository.findById(uid);
        if (!userOpt.isPresent()) {
            return false;
        }
        
        Optional<FcmToken> existingToken = fcmTokenRepository.findByUserUidAndDeviceId(uid, deviceId);
        
        if (existingToken.isPresent()) {
            
            FcmToken token = existingToken.get();
            token.setFcmToken(fcmToken);
            token.setDeviceType(deviceType);
            token.setIsActive("Y");
            fcmTokenRepository.save(token);
        } else {
           
            FcmToken newToken = FcmToken.builder()
                .user(userOpt.get())
                .fcmToken(fcmToken)
                .deviceType(deviceType)
                .deviceId(deviceId)
                .isActive("Y")
                .build();
            fcmTokenRepository.save(newToken);
        }
        
        return true;
    }
    
    public List<FcmToken> getActiveTokensByUserId(String uid) {
        return fcmTokenRepository.findActiveTokensByUserId(uid);
    }
    
    public void deactivateToken(String fcmToken) {
        Optional<FcmToken> tokenOpt = fcmTokenRepository.findByFcmToken(fcmToken);
        if (tokenOpt.isPresent()) {
            FcmToken token = tokenOpt.get();
            token.setIsActive("N");
            fcmTokenRepository.save(token);
        }
    }
}