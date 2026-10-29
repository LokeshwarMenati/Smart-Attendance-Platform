package com.smartattendance.platform.security.service;

import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.model.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        Employee employee = employeeRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found with email: " + email));

        return UserPrincipal.from(employee);
    }
}

