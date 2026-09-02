package com.asalkartechworks.kshitijascreations.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlaceOrderRequest(
        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 150, message = "Customer name must be between 2 and 150 characters")
        String customerName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian mobile number")
        String mobileNumber,

        @NotBlank(message = "Address is required")
        @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
        String addressLine,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Postal code must be exactly 6 digits")
        String postalCode,

        @Size(max = 500, message = "Customer note must not exceed 500 characters")
        String customerNote,

        @NotEmpty(message = "At least one item is required")
        @Size(max = 30, message = "Maximum 30 different items per order")
        @Valid
        List<OrderItemRequest> items
) {
}
