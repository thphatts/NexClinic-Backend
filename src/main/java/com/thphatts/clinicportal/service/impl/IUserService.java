package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.dto.response.UserResponse;
import com.thphatts.clinicportal.dto.request.UserRequest;
import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.mapper.UserMapper;
import com.thphatts.clinicportal.repository.UserRepository;
import com.thphatts.clinicportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IUserService implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public void create(UserRequest rq) {
        userRepository.save(mapToEntity(rq));
    }

    @Override
    public void update(String id, UserRequest rq) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new RuntimeException("User not exits!");
        }
        User userUpdate = user.get();
        userUpdate.setName(rq.getName());
        userUpdate.setEmail(rq.getEmail());
        userUpdate.setPassword(rq.getPassword());
        userUpdate.setUsername(rq.getUsername());
        userRepository.save(userUpdate);
    }

    @Override
    public List<UserResponse> index() {
        List<User> list = userRepository.findAll();
        return list.stream().map(this::mapToResponse).toList();
    }

    @Override
    public void delete(String id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new RuntimeException("User not exits!");
        }
        userRepository.delete(user.get());
    }

    private UserResponse mapToResponse(User user) {
        return userMapper.toResponse(user);
    }

    private User mapToEntity(UserRequest request) {
        return userMapper.toEntity(request);
    }
}