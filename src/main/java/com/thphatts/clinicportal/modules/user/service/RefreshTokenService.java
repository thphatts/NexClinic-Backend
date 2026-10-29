package com.thphatts.clinicportal.modules.user.service;

public interface RefreshTokenService {
     String createRefreshToken(String userId);
     String validateAndRotate(String rawToken);
     void revoke(String rawToken);
}
