package com.Klex.reportingService.scheduler.service;

import com.Klex.reportingService.scheduler.model.OutputFormat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ReportDeliveryService {

    private final JavaMailSender mailSender;

    public ReportDeliveryService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(byte[] reportBytes, String emailTo, OutputFormat outputFormat) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(emailTo);
            helper.setSubject("Generated Report");
            helper.setText("Please find the attached report.");

            String fileName = "report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                    "." + outputFormat.name().toLowerCase();

            helper.addAttachment(fileName, () -> new java.io.ByteArrayInputStream(reportBytes));

            mailSender.send(message);
            log.info("Report sent via email to: {}", emailTo);
        } catch (javax.mail.MessagingException e) {
            log.error("Failed to send email to: {}", emailTo, e);
            throw e;
        }
    }

    public void sendEmailWithMultipleAttachments(java.util.List<byte[]> reportsBytes, java.util.List<String> fileNames, String emailTo) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(emailTo);
            helper.setSubject("Generated Reports");
            helper.setText("Please find the attached reports.");

            for (int i = 0; i < reportsBytes.size(); i++) {
                final byte[] bytes = reportsBytes.get(i);
                String fileName = fileNames.get(i);
                helper.addAttachment(fileName, () -> new java.io.ByteArrayInputStream(bytes));
            }

            mailSender.send(message);
            log.info("Reports sent via email to: {}", emailTo);
        } catch (javax.mail.MessagingException e) {
            log.error("Failed to send email to: {}", emailTo, e);
            throw e;
        }
    }
}
