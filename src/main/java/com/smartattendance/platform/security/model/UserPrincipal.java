package com.smartattendance.platform.security.model;

import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final Long internalId;
    private final UUID publicId;
    private final String employeeCode;
    private final String email;
    private final String password;
    private final EmployeeRole role;
    private final EmployeeStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long internalId,
                         UUID publicId,
                         String employeeCode,
                         String email,
                         String password,
                         EmployeeRole role,
                         EmployeeStatus status,
                         Collection<? extends GrantedAuthority> authorities) {
        this.internalId = internalId;
        this.publicId = publicId;
        this.employeeCode = employeeCode;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.authorities = authorities;
    }

    public static UserPrincipal from(Employee employee) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()));
        return new UserPrincipal(
                employee.getId(),
                employee.getPublicId(),
                employee.getEmployeeCode(),
                employee.getEmail(),
                employee.getPasswordHash(),
                employee.getRole(),
                employee.getStatus(),
                authorities
        );
    }

    public Long getInternalId() {
        return internalId;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public EmployeeStatus getStatus() {
        return status;
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
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == EmployeeStatus.ACTIVE;
    }
}

