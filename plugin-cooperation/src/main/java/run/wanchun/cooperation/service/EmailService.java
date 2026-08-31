package run.wanchun.cooperation.service;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.wanchun.cooperation.config.CooperationProperties;
import run.wanchun.cooperation.extension.Cooperation;

/**
 * 邮件发送服务，配置来自插件设置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final CooperationProperties properties;

    /**
     * 发送合作咨询邮件，若未配置 SMTP 则跳过并返回成功（仅落库不发信）。
     */
    public Mono<Boolean> sendCooperationEmail(Cooperation cooperation) {
        String host = properties.getSmtpHost() != null ? properties.getSmtpHost().trim() : "";
        String receiver = properties.getReceiverEmail() != null
            ? properties.getReceiverEmail().trim() : "";

        if (host.isEmpty() || receiver.isEmpty()) {
            log.warn("SMTP 未配置完整（host={}, receiver={}），跳过发信，仅落库", host, receiver);
            return Mono.just(true);
        }

        int port = properties.getSmtpPort() != null ? properties.getSmtpPort() : 465;
        String user = properties.getSmtpUsername() != null ? properties.getSmtpUsername().trim() : "";
        String pass = properties.getSmtpPassword() != null ? properties.getSmtpPassword() : "";
        String from = properties.getFromEmail() != null && !properties.getFromEmail().isBlank()
            ? properties.getFromEmail().trim()
            : user;
        boolean useSsl = properties.getSmtpSsl() == null || Boolean.TRUE.equals(properties.getSmtpSsl());

        return Mono.fromCallable(() -> {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            if (!user.isEmpty()) {
                sender.setUsername(user);
                sender.setPassword(pass);
            }
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", String.valueOf(!user.isEmpty()));
            if (useSsl) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.connectiontimeout", "5000");

            Cooperation.CooperationSpec spec = cooperation.getSpec();
            String typeLabel = spec.getTypeLabel() != null && !spec.getTypeLabel().isBlank()
                ? spec.getTypeLabel() : spec.getType();
            String subject = "【万椿官网】合作咨询 - " + spec.getCompany() + " - " + typeLabel;
            String text = String.join("\n",
                "公司名称：" + spec.getCompany(),
                "联系人：" + spec.getContact(),
                "联系电话：" + spec.getPhone(),
                "合作类型：" + typeLabel + " (" + spec.getType() + ")",
                "合作意向：" + (spec.getMessage() != null && !spec.getMessage().isBlank()
                    ? spec.getMessage() : "（未填写）"),
                "来源页面：" + (spec.getSourceUrl() != null ? spec.getSourceUrl() : "-"),
                "User-Agent：" + (spec.getUserAgent() != null ? spec.getUserAgent() : "-"),
                "提交时间：" + (spec.getTimestamp() != null ? spec.getTimestamp() : "-"),
                "提交 IP：" + (spec.getIp() != null ? spec.getIp() : "-")
            );

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from.isEmpty() ? user : from);
            helper.setTo(receiver);
            helper.setSubject(subject);
            helper.setText(text);
            sender.send(message);
            log.info("合作咨询邮件已发送至 {}，公司：{}", receiver, spec.getCompany());
            return true;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).onErrorMap(e -> {
            log.error("发送合作咨询邮件失败", e);
            return new RuntimeException("邮件发送失败，请稍后重试", e);
        });
    }
}
