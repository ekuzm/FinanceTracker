package com.finance.tracker.controller;

import com.finance.tracker.controller.api.UserControllerApi;
import com.finance.tracker.domain.AccountType;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.service.UserService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerApi {

    private final UserService service;

    @GetMapping("/api/v1/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getUserById(id));
    }

    @GetMapping("/api/v1/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @GetMapping("/api/v1/users/search/account-type/jpql")
    public ResponseEntity<List<UserResponse>> searchUsersWithJpql(
            @RequestParam("accountType") AccountType accountType,
            @RequestParam("minBudgetLimit") BigDecimal minBudgetLimit,
            @RequestParam("maxBudgetLimit") BigDecimal maxBudgetLimit) {
        return ResponseEntity.ok(service.searchUsersByAccountTypeWithJpql(
                accountType,
                minBudgetLimit,
                maxBudgetLimit));
    }

    @GetMapping("/api/v1/users/search/account-type/native")
    public ResponseEntity<List<UserResponse>> searchUsersWithNative(
            @RequestParam("accountType") AccountType accountType,
            @RequestParam("minBudgetLimit") BigDecimal minBudgetLimit,
            @RequestParam("maxBudgetLimit") BigDecimal maxBudgetLimit) {
        return ResponseEntity.ok(service.searchUsersByAccountTypeWithNative(
                accountType,
                minBudgetLimit,
                maxBudgetLimit));
    }

    @PostMapping("/api/v1/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = service.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api/v1/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable("id") Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(service.updateUser(id, request));
    }

    @DeleteMapping("/api/v1/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
