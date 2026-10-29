package com.smartattendance.platform.common.exception;

import com.smartattendance.platform.common.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler producing RFC 9457 Problem Details responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://smartattendance.com/errors/not-found"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setType(URI.create("https://smartattendance.com/errors/conflict"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ProblemDetail handleInvalidOperation(InvalidOperationException ex, HttpServletRequest request) {
        log.warn("Invalid operation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Business Operation");
        problem.setType(URI.create("https://smartattendance.com/errors/bad-request"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Database data integrity violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "A conflicting record already exists in the database. Duplicate action rejected."
        );
        problem.setTitle("Data Integrity Conflict");
        problem.setType(URI.create("https://smartattendance.com/errors/conflict"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error processing request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Input validation failed for one or more fields."
        );
        problem.setTitle("Validation Failure");
        problem.setType(URI.create("https://smartattendance.com/errors/validation-failure"));

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("validationErrors", fieldErrors);
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on resource: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not possess the required permissions to access this resource."
        );
        problem.setTitle("Access Forbidden");
        problem.setType(URI.create("https://smartattendance.com/errors/forbidden"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(org.springframework.security.authentication.BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed: bad credentials");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password."
        );
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://smartattendance.com/errors/unauthorized"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ProblemDetail handleDisabledException(org.springframework.security.authentication.DisabledException ex, HttpServletRequest request) {
        log.warn("Authentication failed: account disabled - {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        problem.setTitle("Account Deactivated");
        problem.setType(URI.create("https://smartattendance.com/errors/account-deactivated"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication credentials were missing, invalid, or expired."
        );
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://smartattendance.com/errors/unauthorized"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred. Please contact system support."
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://smartattendance.com/errors/internal-server-error"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    private void enrichProblemDetail(ProblemDetail problem, HttpServletRequest request) {
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
    }
}

