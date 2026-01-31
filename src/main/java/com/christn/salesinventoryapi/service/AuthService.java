package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.LoginRequest;
import com.christn.salesinventoryapi.dto.request.LogoutRequest;
import com.christn.salesinventoryapi.dto.request.RefreshRequest;
import com.christn.salesinventoryapi.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request, String ip, String userAgent);

    AuthResponse refresh(RefreshRequest request, String ip, String userAgent);

    void logout(LogoutRequest request);
}
