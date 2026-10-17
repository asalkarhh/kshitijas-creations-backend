package com.asalkartechworks.kshitijascreations.push;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminPushSubscriptionRepository extends JpaRepository<AdminPushSubscription, UUID> {
    Optional<AdminPushSubscription> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}

