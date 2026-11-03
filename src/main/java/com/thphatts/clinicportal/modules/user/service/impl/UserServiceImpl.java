package com.thphatts.clinicportal.modules.user.service.impl;

import com.thphatts.clinicportal.modules.user.dto.UserRequest;
import com.thphatts.clinicportal.modules.user.dto.UserResponse;
import com.thphatts.clinicportal.modules.user.entity.User;
import com.thphatts.clinicportal.modules.user.mapper.UserMapper;
import com.thphatts.clinicportal.modules.user.repository.UserRepository;
import com.thphatts.clinicportal.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import com.thphatts.clinicportal.modules.user.entity.enums.Role;
import com.thphatts.clinicportal.modules.person.repository.PersonRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PersonRepository personRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void create(UserRequest rq) {
        String username = rq.username().trim();
        String email = rq.email().trim().toLowerCase();
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Username hoặc email đã được đăng ký.");
        User user = mapToEntity(rq);
        user.setUsername(username);
        user.setEmail(email);
        if (rq.phone() != null && !rq.phone().isBlank()) {
            String phone = normalizePhone(rq.phone());
            var existingPerson = personRepository.findByPhoneNumber(phone);
            if (existingPerson.isPresent()) {
                if (userRepository.existsByPerson_Id(existingPerson.get().getId()))
                    throw new IllegalArgumentException("Số điện thoại này đã có tài khoản.");
                user.setPerson(existingPerson.get());
            }
        }
        user.setPassword(passwordEncoder.encode(rq.password()));
        if (rq.role() != null) {
            user.setRole(rq.role());
        } else {
            user.setRole(Role.ROLE_PATIENT);
        }
        userRepository.save(user);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void update(String id, UserRequest rq) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new RuntimeException("User not exits!");
        }
        User userUpdate = user.get();
        String email = rq.email().trim().toLowerCase();
        userRepository.findByEmail(email).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new IllegalArgumentException("Email đã được đăng ký.");
        });
        userRepository.findByUsername(rq.username().trim()).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new IllegalArgumentException("Username đã được đăng ký.");
        });
        if (rq.phone() != null && !rq.phone().isBlank()) {
            String phone = normalizePhone(rq.phone());
            personRepository.findByPhoneNumber(phone).filter(other -> !other.getId().equals(userUpdate.getPerson().getId()))
                    .ifPresent(other -> { throw new IllegalArgumentException("Số điện thoại đã thuộc về Person khác."); });
            userUpdate.getPerson().setPhoneNumber(phone);
        }
        userUpdate.getPerson().setFullName(rq.name());
        userUpdate.getPerson().setAddress(rq.address());
        userUpdate.getPerson().setCitizenId(rq.citizenId());
        userUpdate.getPerson().setEmail(email);
        userUpdate.setEmail(email);
        if (rq.password() != null && !rq.password().isBlank())
            userUpdate.setPassword(passwordEncoder.encode(rq.password()));
        userUpdate.setUsername(rq.username().trim());
        if (rq.role() != null) userUpdate.setRole(rq.role());
        personRepository.save(userUpdate.getPerson());
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

    private String normalizePhone(String phone) {
        return phone.replaceAll("[\\s()-]", "").trim();
    }
}
