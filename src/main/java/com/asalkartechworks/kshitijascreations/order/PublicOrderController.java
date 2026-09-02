package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.order.dto.PlaceOrderRequest;
import com.asalkartechworks.kshitijascreations.order.dto.PlaceOrderResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/orders")
public class PublicOrderController {

    private final OrderService orderService;
    private final com.asalkartechworks.kshitijascreations.order.OrderRateLimiter rateLimiter;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceOrderResponse placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String clientIp = getClientIp(httpRequest);
        if (!rateLimiter.isAllowed(clientIp)) {
            throw new com.asalkartechworks.kshitijascreations.api.ApiException(
                    HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", 
                    "Too many orders placed recently. Please try again later.", Map.of());
        }
        return orderService.placeOrder(request);
    }
    
    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
