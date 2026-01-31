package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import com.christn.salesinventoryapi.dto.mapper.UserMapper;
import com.christn.salesinventoryapi.dto.request.CreateUserRequest;
import com.christn.salesinventoryapi.dto.request.UpdateUserRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.UserResponse;
import com.christn.salesinventoryapi.model.Role;
import com.christn.salesinventoryapi.model.User;
import com.christn.salesinventoryapi.repository.UserRepository;
import com.christn.salesinventoryapi.repository.spec.UserSpecifications;
import com.christn.salesinventoryapi.service.AdminUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo electrónico");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(request.enabled() == null || request.enabled())
                .roles(request.roles())
                .build();

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, AuthUserDetails principal) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        // Regla 1: no desactivarse a sí mismo
        if (id.equals(principal.getId()) && request.enabled() != null && request.enabled().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException("No puedes deshabilitar tu propio usuario");
        }

        if (request.email() != null && !user.getEmail().equalsIgnoreCase(request.email())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Ya existe un usuario con ese correo electrónico");
            }
            user.setEmail(request.email());
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        // Regla 2: Admin no puede quitarse el rol de ADMIN
        if (id.equals(principal.getId()) && request.roles() != null && !request.roles().contains(Role.ADMIN)) {
            throw new IllegalArgumentException("No puedes quitarte el rol ADMIN a ti mismo");
        }

        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(request.roles());
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public PageResponse<UserResponse> search(Boolean enabled, String email, LocalDateTime from,
            LocalDateTime to, Set<Role> roles, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecifications.isEnabled(enabled));

        if (email != null && !email.isBlank()) spec = spec.and(UserSpecifications.emailContains(email));
        if (from != null) spec = spec.and(UserSpecifications.createdAtFrom(from));
        if (to != null) spec = spec.and(UserSpecifications.createdAtTo(to));
        if (roles != null && !roles.isEmpty()) spec = spec.and(UserSpecifications.hasAnyRole(roles));

        Page<UserResponse> page = userRepository
                .findAll(spec, pageable)
                .map(UserMapper::toResponse);

        return PageResponse.from(page);
    }
}
