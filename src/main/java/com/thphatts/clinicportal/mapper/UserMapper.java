package com.thphatts.clinicportal.mapper;

import com.thphatts.clinicportal.dto.record.UserResponse;
import com.thphatts.clinicportal.dto.record.UserRequest;
import com.thphatts.clinicportal.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    @Mapping(source = "phoneNumber", target = "phone")
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "phone", target = "phoneNumber")
    User toEntity(UserRequest request);
}
