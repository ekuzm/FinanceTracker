package com.finance.tracker.service;

import com.finance.tracker.domain.AccountType;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.response.UserResponse;
import java.math.BigDecimal;
import java.util.List;

public interface UserService {
    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    List<UserResponse> searchUsersByAccountTypeWithJpql(
            AccountType accountType, BigDecimal minBudgetLimit, BigDecimal maxBudgetLimit);

    List<UserResponse> searchUsersByAccountTypeWithNative(
            AccountType accountType, BigDecimal minBudgetLimit, BigDecimal maxBudgetLimit);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserUpdateRequest user);

    void deleteUser(Long id);
}
