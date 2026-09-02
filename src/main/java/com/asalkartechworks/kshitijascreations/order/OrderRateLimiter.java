package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.config.AppProperties;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OrderRateLimiter {

    private final int limitPerHour;
    private final Map<String, RateRecord> records = new ConcurrentHashMap<>();

    public OrderRateLimiter(AppProperties properties) {
        this.limitPerHour = properties.getSecurity().getOrderRateLimitPerHour();
    }

    public boolean isAllowed(String ipAddress) {
        Instant now = Instant.now();
        Instant hourAgo = now.minusSeconds(3600);
        
        RateRecord record = records.compute(ipAddress, (k, v) -> {
            if (v == null || v.windowStart.isBefore(hourAgo)) {
                return new RateRecord(1, now);
            }
            return new RateRecord(v.count + 1, v.windowStart);
        });

        return record.count <= limitPerHour;
    }

    private record RateRecord(int count, Instant windowStart) {}
}

