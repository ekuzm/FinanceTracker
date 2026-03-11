package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request body for partially updating a user.")
public class UserUpdateRequest {

    @Pattern(regexp = "\\s*+\\S.*", message = "must not be blank")
    @Size(min = 3, max = 50)
    private String username;

    @Email
    private String email;

    private List<Long> accountIds;

    private List<Long> budgetIds;
}
