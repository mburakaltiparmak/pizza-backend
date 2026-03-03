package com.example.pizza.service.logic;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.OrderItem;
import com.example.pizza.entity.product.Product;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Properties;

public class RealEmailManualTest {

    @Test
    void sendRealEmail_ManualSetup() throws Exception {
        // 1. Load .env
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Check if we have necessary config
        String host = dotenv.get("MAIL_HOST");
        String port = dotenv.get("MAIL_PORT");
        String username = dotenv.get("MAIL_USERNAME");
        String password = dotenv.get("MAIL_PASSWORD"); // Note: in .env it might be MAIL_PASSWORD or similar

        // Fallback or check system properties if dotenv is empty (e.g. if run in
        // environment with env vars already set)
        if (host == null)
            host = System.getenv("MAIL_HOST");
        if (port == null)
            port = System.getenv("MAIL_PORT");
        if (username == null)
            username = System.getenv("MAIL_USERNAME");
        if (password == null)
            password = System.getenv("MAIL_PASSWORD");

        if (host == null || username == null || password == null) {
            System.out.println(
                    "⚠️ MAIL_HOST, MAIL_USERNAME, or MAIL_PASSWORD not found in .env or environment. Skipping test.");
            return;
        }

        // 2. Configure JavaMailSender
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(Integer.parseInt(port != null ? port : "587"));
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.debug", "true"); // Enable for debugging
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        if ("465".equals(port)) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true"); // Implicit SSL
            props.put("mail.smtp.socketFactory.port", port);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true"); // STARTTLS
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.ssl.checkserveridentity", "false");

            // FORCE TRUST MANAGER
            try {
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, new javax.net.ssl.TrustManager[] { new javax.net.ssl.X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                } }, new java.security.SecureRandom());

                mailSender.setJavaMailProperties(props);
                // We can't easily inject the socket factory into JavaMailSenderImpl without
                // setting it as default
                // But setting the property "mail.smtp.ssl.socketFactory" uses a class name,
                // effectively hard to pass an instance.
                // However, JavaMail supports "mail.smtp.socketFactory" which we can set via
                // props if we use a custom class or reliance on default context.
                // The easier way is to rely on "mail.smtp.ssl.trust" property which SHOULD work
                // if the JavaMail version supports it.
                // If it failed before, maybe we need "mail.smtp.ssl.protocols" set to
                // "TLSv1.2".
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 3. Instantiate EmailService
        EmailService emailService = new EmailService(mailSender);

        // 4. Inject @Value fields via Reflection
        setPrivateField(emailService, "fromEmail", "info@burakaltiparmak.site"); // Default or from env
        setPrivateField(emailService, "baseUrl", "http://localhost:8080");
        setPrivateField(emailService, "frontendUrl", "http://localhost:3000");

        // 5. Prepare Order Data
        String recipientEmail = System.getProperty("test.email.recipient", "TEST_EMAIL_UNDEFINED");
        if ("TEST_EMAIL_UNDEFINED".equals(recipientEmail)) {
            // Try to get from args or default
            recipientEmail = "mburakaltiparmak@gmail.com";
            System.out.println("No recipient specified via system property, using default target: " + recipientEmail);
        }

        System.out.println("Attempting to send real email to: " + recipientEmail);

        Order order = new Order();
        order.setId(888L);
        order.setTotalAmount(250.0);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setGuestEmail(recipientEmail);

        com.example.pizza.entity.user.UserAddress address = new com.example.pizza.entity.user.UserAddress();
        address.setRecipientName("Manual Test User");
        address.setFullAddress("Manual Test St 123");
        address.setCity("Istanbul");
        address.setDistrict("Kadikoy");
        order.setDeliveryAddress(address);

        Product product = new Product();
        product.setName("Manual Context Pizza");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setPrice(125.0);
        order.setItems(Collections.singletonList(item));

        // 6. Send Email
        try {
            emailService.sendOrderConfirmationEmail(order, null);
            System.out.println("✅ Email sent successfully (manual context)!");
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
