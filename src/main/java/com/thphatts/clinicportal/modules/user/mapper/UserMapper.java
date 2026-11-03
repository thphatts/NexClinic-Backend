package com.thphatts.clinicportal.modules.user.mapper;

import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.user.dto.UserRequest;
import com.thphatts.clinicportal.modules.user.dto.UserResponse;
import com.thphatts.clinicportal.modules.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        Person person = user.getPerson();
        return new UserResponse(user.getId(), person == null ? null : person.getFullName(),
                person == null ? null : person.getPhoneNumber(), user.getEmail(), user.getUsername(),
                person == null ? null : person.getAddress(), person == null ? null : person.getCitizenId());
    }

    public User toEntity(UserRequest request) {
        Person person = new Person();
        person.setFullName(request.name());
        person.setPhoneNumber(request.phone());
        person.setEmail(request.email());
        person.setAddress(request.address());
        person.setCitizenId(request.citizenId());
        User user = new User();
        user.setPerson(person);
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setRole(request.role());
        return user;
    }
}
