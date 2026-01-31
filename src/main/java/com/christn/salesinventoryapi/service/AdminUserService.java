package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import com.christn.salesinventoryapi.dto.request.CreateUserRequest;
import com.christn.salesinventoryapi.dto.request.UpdateUserRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.UserResponse;
import com.christn.salesinventoryapi.model.Role;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Set;

public interface AdminUserService {

    UserResponse create(CreateUserRequest request);

    UserResponse findById(Long id);

    UserResponse update(Long id, UpdateUserRequest request, AuthUserDetails principal);

    PageResponse<UserResponse> search(
            Boolean enabled,
            String email,
            LocalDateTime from,
            LocalDateTime to,
            Set<Role> roles,
            Pageable pageable);
}
