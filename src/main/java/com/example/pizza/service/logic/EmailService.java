package com.example.pizza.service.logic;

import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.OrderItem;
import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.entity.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.from-address:info@burakaltiparmak.site}")
    private String fromEmail;

    @Async("emailTaskExecutor")
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("Teknolojik Yemekler <" + fromEmail + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            log.info("📧 Simple email sent to {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send simple email to {}: {}", to, e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    public void sendVerificationEmail(User user, String token) {
        // Redirect to frontend verification page for better UX
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                +
                "<h2 style='color: #e63946;'>E-posta Adresinizi Doğrulayın</h2>" +
                "<p>Merhaba <strong>" + user.getName() + "</strong>,</p>" +
                "<p>Hesabınızı doğrulamak için aşağıdaki butona tıklayın:</p>" +
                "<p style='text-align: center;'>" +
                "<a href='" + verificationUrl
                + "' style='display: inline-block; background-color: #e63946; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>E-posta Adresimi Doğrula</a>"
                +
                "</p>" +
                "<p>Bu bağlantı 24 saat geçerlidir.</p>" +
                "<p>İyi günler dileriz,<br>Teknolojik Yemekler Ekibi</p>" +
                "</div>";

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, "Teknolojik Yemekler"));
            helper.setTo(user.getEmail());
            helper.setSubject("E-posta Doğrulama - Teknolojik Yemekler");
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("📧 Verification email sent to {}", user.getEmail());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("❌ SMTP ERROR - Failed to send verification email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendOrderConfirmationEmail(Order order, User user) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String orderDate = dateFormat.format(new Date());

        String userName = order.getOrderName();
        String userEmail = order.getOrderEmail();

        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemsHtml.append("<tr>")
                    .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
                    .append(item.getProduct().getName()).append("</td>")
                    .append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: center;'>")
                    .append(item.getQuantity()).append("</td>")
                    .append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: right;'>")
                    .append(String.format("%.2f ₺", item.getPrice())).append("</td>")
                    .append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: right;'>")
                    .append(String.format("%.2f ₺", item.getPrice() * item.getQuantity())).append("</td>")
                    .append("</tr>");
        }

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                +
                "<h2 style='color: #e63946; text-align: center;'>🍕 Siparişiniz Alındı!</h2>" +
                "<p>Merhaba <strong>" + userName + "</strong>,</p>" +
                "<p>Siparişiniz başarıyla alındı. Detaylar aşağıdadır:</p>" +
                "<div style='background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p style='margin: 5px 0;'><strong>Sipariş No:</strong> #" + order.getId() + "</p>" +
                "<p style='margin: 5px 0;'><strong>Tarih:</strong> " + orderDate + "</p>" +
                "<p style='margin: 5px 0;'><strong>Toplam Tutar:</strong> "
                + String.format("%.2f ₺", order.getTotalAmount()) + "</p>" +
                "<p style='margin: 5px 0;'><strong>Durum:</strong> " + getOrderStatusInTurkish(order.getOrderStatus())
                + "</p>" +
                "</div>" +
                "<h3 style='color: #e63946;'>Sipariş Detayları</h3>" +
                "<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>" +
                "<thead>" +
                "<tr style='background-color: #e63946; color: white;'>" +
                "<th style='padding: 10px; text-align: left;'>Ürün</th>" +
                "<th style='padding: 10px; text-align: center;'>Adet</th>" +
                "<th style='padding: 10px; text-align: right;'>Birim Fiyat</th>" +
                "<th style='padding: 10px; text-align: right;'>Toplam</th>" +
                "</tr>" +
                "</thead>" +
                "<tbody>" +
                itemsHtml.toString() +
                "</tbody>" +
                "</table>" +
                "<div style='background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #ffc107;'>"
                +
                "<p style='margin: 0;'><strong>📍 Teslimat Adresi:</strong></p>" +
                "<p style='margin: 5px 0 0 0;'>" + order.getDeliveryAddress().getFullAddress() + ", " +
                order.getDeliveryAddress().getDistrict() + "/" + order.getDeliveryAddress().getCity() + "</p>" +
                "</div>" +
                "<p style='text-align: center; color: #666; margin-top: 30px;'>Siparişiniz en kısa sürede hazırlanacak ve size teslim edilecektir. Afiyet olsun! 🍕</p>"
                +
                "<p style='text-align: center; color: #999; font-size: 12px; margin-top: 20px;'>Herhangi bir sorunuz varsa, lütfen bizimle iletişime geçin. Teşekkür ederiz! 🙏</p>"
                +
                "<p>İyi günler dileriz,<br><strong>Teknolojik Yemekler Ekibi</strong> 🍕</p>" +
                "</div>";

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, "Teknolojik Yemekler"));
            helper.setTo(userEmail);
            helper.setSubject("🍕 Sipariş Onayı - #" + order.getId() + " - Teknolojik Yemekler");
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("📧 Order confirmation email sent to {} for order #{}", userEmail, order.getId());
        } catch (MessagingException e) {
            log.error("Failed to send order confirmation email to {}: {}", userEmail, e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendOrderStatusUpdateEmail(Order order, User user, OrderStatus status) {
        String userName = order.getOrderName();
        String userEmail = order.getOrderEmail();
        String statusText = getOrderStatusInTurkish(status);
        String subject = "";
        String messageIntro = "";
        String emoji = "";

        switch (status) {
            case PREPARING:
                subject = "👨‍🍳 Siparişiniz Hazırlanıyor - #" + order.getId();
                messageIntro = "Siparişiniz şu anda mutfakta hazırlanıyor. Çok yakında yola çıkacak!";
                emoji = "👨‍🍳";
                break;
            case SHIPPING:
                subject = "🚚 Siparişiniz Yolda - #" + order.getId();
                messageIntro = "Siparişiniz şu anda size doğru yola çıktı. Kısa süre içinde teslim edilecek.";
                emoji = "🚚";
                break;
            case DELIVERED:
                subject = "✅ Siparişiniz Teslim Edildi - #" + order.getId();
                messageIntro = "Siparişiniz başarıyla teslim edildi. Afiyet olsun!";
                emoji = "✅";
                break;
            case CANCELLED:
                subject = "❌ Siparişiniz İptal Edildi - #" + order.getId();
                messageIntro = "Siparişiniz iptal edildi. Daha fazla bilgi için lütfen bizimle iletişime geçin.";
                emoji = "❌";
                break;
            default:
                subject = "📋 Sipariş Durumu Güncellendi - #" + order.getId();
                messageIntro = "Siparişinizin durumu '" + statusText + "' olarak güncellendi.";
                emoji = "📋";
                break;
        }

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                +
                "<h2 style='color: #e63946; text-align: center;'>" + emoji + " Sipariş Durumu Güncellendi</h2>" +
                "<p>Merhaba <strong>" + userName + "</strong>,</p>" +
                "<p>" + messageIntro + "</p>" +
                "<div style='background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p style='margin: 5px 0;'><strong>Sipariş No:</strong> #" + order.getId() + "</p>" +
                "<p style='margin: 5px 0;'><strong>Yeni Durum:</strong> <span style='color: #e63946;'>" + statusText
                + "</span></p>" +
                "<p style='margin: 5px 0;'><strong>Toplam Tutar:</strong> "
                + String.format("%.2f ₺", order.getTotalAmount()) + "</p>" +
                "</div>" +
                "<p style='text-align: center; color: #666; margin-top: 30px;'>Siparişinizle ilgili güncellemeler size email ile bildirilecektir.</p>"
                +
                "<p style='text-align: center; color: #999; font-size: 12px; margin-top: 20px;'>Herhangi bir sorunuz varsa, lütfen bizimle iletişime geçin. Teşekkür ederiz! 🙏</p>"
                +
                "<p>İyi günler dileriz,<br><strong>Teknolojik Yemekler Ekibi</strong> 🍕</p>" +
                "</div>";

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, "Teknolojik Yemekler"));
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("📧 Order status update email sent to {} for order #{}", userEmail, order.getId());
        } catch (MessagingException e) {
            log.error("Failed to send order status update email to {}: {}", userEmail, e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(User user, String token) {
        // Use frontend URL for password reset (user interaction page)
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                +
                "<h2 style='color: #e63946;'>🔑 Şifrenizi Sıfırlayın</h2>" +
                "<p>Merhaba <strong>" + user.getName() + "</strong>,</p>" +
                "<p>Şifrenizi sıfırlamak için aşağıdaki butona tıklayın:</p>" +
                "<p style='text-align: center;'>" +
                "<a href='" + resetUrl
                + "' style='display: inline-block; background-color: #e63946; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>🔑 Şifremi Sıfırla</a>"
                +
                "</p>" +
                "<p>Bu bağlantı 1 saat geçerlidir.</p>" +
                "<p style='color: #666; font-size: 12px;'>Eğer şifre sıfırlama talebinde bulunmadıysanız, bu emaili görmezden gelebilirsiniz.</p>"
                +
                "<p>İyi günler dileriz,<br>Teknolojik Yemekler Ekibi</p>" +
                "</div>";

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, "Teknolojik Yemekler"));
            helper.setTo(user.getEmail());
            helper.setSubject("🔑 Şifre Sıfırlama - Teknolojik Yemekler");
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("📧 Password reset email sent to {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getOrderStatusInTurkish(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Beklemede";
            case CONFIRMED -> "Onaylandı";
            case PREPARING -> "Hazırlanıyor";
            case READY -> "Hazır";
            case SHIPPING -> "Yolda";
            case DELIVERED -> "Teslim Edildi";
            case CANCELLED -> "İptal Edildi";
        };
    }
}