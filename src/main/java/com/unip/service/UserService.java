package com.unip.service;

import com.unip.model.User;

import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    User findById(Long id);
    Optional<User> findByEmail(String email);
    boolean checkFaceId(String faceId);
}