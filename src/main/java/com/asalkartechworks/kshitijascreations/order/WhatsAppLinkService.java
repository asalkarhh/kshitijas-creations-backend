package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.config.AppProperties;
import com.asalkartechworks.kshitijascreations.domain.entity.Order;
import com.asalkartechworks.kshitijascreations.domain.entity.OrderItem;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppLinkService {

    private static final Locale INDIA = Locale.of("en", "IN");
    private final String adminNumber;

    public WhatsAppLinkService(AppProperties appProperties) {
        this.adminNumber = appProperties.getWhatsapp().getAdminNumber();
    }

    public String generateLink(Order order, List<OrderItem> items) {
        String message = buildMessage(order, items);
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + adminNumber + "?text=" + encoded;
    }

    private String buildMessage(Order order, List<OrderItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("New Order \u2013 Kshitija's Creations\n\n");
        sb.append("Order: ").append(order.getOrderNumber()).append("\n");
        sb.append("Customer: ").append(org.springframework.web.util.HtmlUtils.htmlEscape(order.getCustomerName())).append("\n");
        sb.append("Mobile: ").append(order.getMobileNumber()).append("\n\n");
        sb.append("Items:\n");

        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            sb.append(i + 1).append(". ")
                    .append(item.getProductName())
                    .append(" \u00d7 ").append(item.getQuantity())
                    .append(" \u2013 ").append(formatCurrency(item.getLineTotal()))
                    .append("\n");
        }

        sb.append("\nTotal: ").append(formatCurrency(order.getTotalAmount())).append("\n\n");
        sb.append("Delivery Address:\n");
        sb.append(org.springframework.web.util.HtmlUtils.htmlEscape(order.getAddressLine())).append(", ");
        sb.append(org.springframework.web.util.HtmlUtils.htmlEscape(order.getCity())).append(", ");
        sb.append(org.springframework.web.util.HtmlUtils.htmlEscape(order.getState())).append(" \u2013 ");
        sb.append(order.getPostalCode());

        if (order.getCustomerNote() != null && !order.getCustomerNote().isBlank()) {
            sb.append("\n\nCustomer Note:\n").append(org.springframework.web.util.HtmlUtils.htmlEscape(order.getCustomerNote()));
        }

        return sb.toString();
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(INDIA);
        return formatter.format(amount);
    }
}
