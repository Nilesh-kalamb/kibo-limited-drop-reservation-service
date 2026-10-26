package com.kibo.drop.service;


import com.kibo.drop.dto.LoginRequest;
import com.kibo.drop.dto.RegisterRequest;

public interface AuthService {
    String register(RegisterRequest request);
    String login(LoginRequest request);
}
