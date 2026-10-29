package com.thphatts.clinicportal.modules.user.service;

import com.thphatts.clinicportal.modules.user.dto.UserRequest;
import com.thphatts.clinicportal.modules.user.dto.UserResponse;

import java.util.List;

public interface UserService {
    void create(UserRequest rq);

    void update(String id, UserRequest rq);

    List<UserResponse> index();
    void delete(String id);
}
