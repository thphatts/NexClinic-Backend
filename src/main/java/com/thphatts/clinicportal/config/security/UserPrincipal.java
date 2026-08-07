package com.thphatts.clinicportal.config.security;

import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.entity.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {
    @Getter
    private final String userId;
    private final String username;
    private final String password;
    @Getter
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.userId = user.getId();
        this.password = user.getPassword();
        this.username = user.getUsername();
        this.role = user.getRole() != null ? user.getRole() : Role.ROLE_PATIENT;
        String roleName = user.getRole() != null ? user.getRole().name() : "ROLE_PATIENT";
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}