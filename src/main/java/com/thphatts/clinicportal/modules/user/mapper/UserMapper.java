package com.thphatts.clinicportal.modules.user.mapper;

import com.thphatts.clinicportal.modules.user.dto.UserRequest;
import com.thphatts.clinicportal.modules.user.dto.UserResponse;
import com.thphatts.clinicportal.modules.user.entity.User;
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
