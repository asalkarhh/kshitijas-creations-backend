package com.asalkartechworks.kshitijascreations.banner;

import java.time.Instant;

public enum BannerStatus {

    ACTIVE,
    INACTIVE,
    SCHEDULED,
    EXPIRED;

    public static BannerStatus compute(boolean active, Instant startAt, Instant endAt, Instant now) {
        if (!active) {
            return INACTIVE;
        }
        if (startAt != null && now.isBefore(startAt)) {
            return SCHEDULED;
        }
        if (endAt != null && now.isAfter(endAt)) {
            return EXPIRED;
        }
        return ACTIVE;
    }
}

