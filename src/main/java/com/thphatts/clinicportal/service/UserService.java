package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.UserResponse;
import com.thphatts.clinicportal.dto.request.UserRequest;

import java.util.List;

public interface UserService {
    void create(UserRequest rq);

    void update(String id, UserRequest rq);

    List<UserResponse> index();
    void delete(String id);
}
