package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.repository.OrderRepository;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final OrderRepository orderRepository;

    public String next() {
        long sequence = orderRepository.nextOrderSequence();
        int year = Year.now().getValue();
        return String.format("KC-%d-%05d", year, sequence);
    }
}
