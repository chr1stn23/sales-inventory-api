package com.christn.salesinventoryapi.dto.mapper;

import com.christn.salesinventoryapi.dto.response.UserResponse;
import com.christn.salesinventoryapi.model.Role;
import com.christn.salesinventoryapi.model.User;

import java.util.stream.Collectors;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
