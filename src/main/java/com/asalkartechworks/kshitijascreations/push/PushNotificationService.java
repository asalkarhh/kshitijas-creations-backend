package com.asalkartechworks.kshitijascreations.push;

import com.asalkartechworks.kshitijascreations.config.AppProperties;
import jakarta.annotation.PostConstruct;
import java.security.Security;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final AppProperties properties;
    private final AdminPushSubscriptionRepository repository;
    private PushService pushService;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("Registered BouncyCastle security provider for Web Push");
        }

        if (properties.getPush().isEnabled()) {
            try {
                this.pushService = new PushService(
                    properties.getPush().getPublicKey(),
                    properties.getPush().getPrivateKey(),
                    properties.getPush().getSubject()
                );
                log.info("Initialized Web Push Service successfully with subject: {}", properties.getPush().getSubject());
            } catch (Exception e) {
                log.error("Failed to initialize Web Push Service with configured VAPID keys", e);
            }
        }
    }

    public String publicKey() {
        return properties.getPush().getPublicKey();
    }

    @Transactional
    public void subscribe(PushSubscriptionRequest request) {
        repository.findByEndpoint(request.endpoint())
            .ifPresentOrElse(
                existing -> {
                    existing.setP256dh(request.p256dh());
                    existing.setAuth(request.auth());
                    repository.save(existing);
                    log.info("Updated existing push subscription for endpoint: {}", maskEndpoint(request.endpoint()));
                },
                () -> {
                    AdminPushSubscription subscription = AdminPushSubscription.builder()
                        .endpoint(request.endpoint())
                        .p256dh(request.p256dh())
                        .auth(request.auth())
                        .build();
                    repository.save(subscription);
                    log.info("Saved new persistent push subscription for endpoint: {}", maskEndpoint(request.endpoint()));
                }
            );
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        repository.deleteByEndpoint(endpoint);
        log.info("Removed push subscription for endpoint: {}", maskEndpoint(endpoint));
    }

    public void notifyNewOrder(String orderNumber) {
        if (!properties.getPush().isEnabled()) {
            log.debug("Push notifications disabled in configuration, skipping");
            return;
        }

        List<AdminPushSubscription> subscriptions = repository.findAll();
        if (subscriptions.isEmpty()) {
            log.warn("No active admin push subscriptions found in database. Make sure admin logged in and allowed notifications.");
            return;
        }

        if (pushService == null) {
            init();
            if (pushService == null) {
                log.error("PushService is not available; cannot deliver notification for order #{}", orderNumber);
                return;
            }
        }

        String payload = "{\"title\":\"🔔 New Order Received\",\"body\":\"Order #" + orderNumber + " received\",\"url\":\"/admin/orders\"}";

        for (AdminPushSubscription sub : subscriptions) {
            try {
                Subscription webPushSubscription = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );
                Notification notification = new Notification(webPushSubscription, payload);
                HttpResponse response = pushService.send(notification);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 201) {
                    log.info("Push notification for order #{} delivered successfully to {}", orderNumber, maskEndpoint(sub.getEndpoint()));
                } else if (statusCode == 410 || statusCode == 404) {
                    log.warn("Push subscription expired on push server (HTTP {}), removing from database: {}", statusCode, maskEndpoint(sub.getEndpoint()));
                    repository.delete(sub);
                } else {
                    log.warn("Push server returned HTTP {} when sending order notification to {}", statusCode, maskEndpoint(sub.getEndpoint()));
                }
            } catch (Exception exception) {
                log.error("Failed to send push notification for order #{} to {}", orderNumber, maskEndpoint(sub.getEndpoint()), exception);
            }
        }
    }

    private String maskEndpoint(String endpoint) {
        if (endpoint == null || endpoint.length() < 30) return endpoint;
        return endpoint.substring(0, 25) + "...";
    }
}
