package com.smartattendance.platform.security.service;

import com.smartattendance.platform.common.exception.InvalidOperationException;
import com.smartattendance.platform.employee.dto.EmployeeResponse;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.dto.LoginRequest;
import com.smartattendance.platform.security.dto.LoginResponse;
import com.smartattendance.platform.security.jwt.JwtTokenProvider;
import com.smartattendance.platform.security.model.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmployeeRepository employeeRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       EmployeeRepository employeeRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.employeeRepository = employeeRepository;
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        Employee employee = employeeRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            log.warn("Login attempt for deactivated employee: {}", normalizedEmail);
            throw new DisabledException("Your employee account is deactivated. Please contact human resources.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for email: {}", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password.");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(
                principal.getPublicId(),
                principal.getUsername(),
                principal.getRole().name(),
                principal.getEmployeeCode()
        );

        log.info("Successful login for employeeCode: {}", principal.getEmployeeCode());

        return new LoginResponse(
                "Bearer",
                token,
                tokenProvider.getExpirationMs() / 1000,
                EmployeeResponse.from(employee)
        );
    }

    public EmployeeResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidOperationException("No authenticated employee in current security context.");
        }

        Employee employee = employeeRepository.findByPublicId(principal.getPublicId())
                .orElseThrow(() -> new InvalidOperationException("Authenticated employee profile not found."));

        return EmployeeResponse.from(employee);
    }
}

