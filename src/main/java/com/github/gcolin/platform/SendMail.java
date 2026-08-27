package com.github.gcolin.platform;

import jakarta.mail.Authenticator;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMail {

    public static final int BATCH_SIZE = 30;
    public static final long BATCH_INTERVAL_MINUTES = 15;

    private String username;
    private String password;
    private ScheduledExecutorService executor;
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private MailTemplate tmpl = new MailTemplate();

    private Properties properties;

    private Config config;

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void init() {
        reloadCredentials();
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void reloadCredentials() {
        this.username = properties.getProperty("mail.USER_NAME");
        this.password = properties.getProperty("mail.PASSWORD");
    }

    public void close() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    private static Properties getProps() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        return props;
    }

    public void send(AbstractMail amail, String to, String subject) throws Exception {
        if (config != null) {
            config.applyOrg(amail);
        }
        String content = tmpl.render(amail.getTemplate(), amail);
        if (password == null || password.isEmpty()) {
            File mailDir = new File("emails");
            mailDir.mkdirs();
            File mailFile = new File(mailDir, mailDir.list().length + ".html");
            logger.info("write into {}", mailFile.getAbsolutePath());
            content = content.replace("<head>", "<head><title>" + to + " - " + subject + "</title>");
            Files.writeString(mailFile.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        } else {
            Session session = Session.getInstance(getProps(), new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            jakarta.mail.Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=UTF-8");

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Transport.send(message);
                    } catch (Exception e) {
                        logger.error(e.toString());
                    }
                }
            });
        }
    }

    public void sendBcc(AbstractMail amail, List<String> bccAddresses, String subject) throws Exception {
        sendBcc(amail, bccAddresses, subject, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Sends BCC emails in batches of {@link #BATCH_SIZE}, waiting
     * {@link #BATCH_INTERVAL_MINUTES} minutes between each batch.
     */
    public int sendBccBatched(AbstractMail amail, List<String> bccAddresses, String subject) throws Exception {
        if (bccAddresses == null || bccAddresses.isEmpty()) {
            return 0;
        }
        int batchIndex = 0;
        for (int i = 0; i < bccAddresses.size(); i += BATCH_SIZE) {
            List<String> batch = new ArrayList<>(bccAddresses.subList(i, Math.min(i + BATCH_SIZE, bccAddresses.size())));
            sendBcc(amail, batch, subject, batchIndex * BATCH_INTERVAL_MINUTES, TimeUnit.MINUTES);
            batchIndex++;
        }
        return bccAddresses.size();
    }

    public void sendBcc(
            AbstractMail amail, List<String> bccAddresses, String subject, long delay, TimeUnit unit)
            throws Exception {
        if (config != null) {
            config.applyOrg(amail);
        }
        String content = tmpl.render(amail.getTemplate(), amail);
        if (password == null || password.isEmpty()) {
            File mailDir = new File("emails");
            mailDir.mkdirs();
            File mailFile = new File(mailDir, mailDir.list().length + ".html");
            logger.info("write bcc into {} (delay {} {})", mailFile.getAbsolutePath(), delay, unit);
            content = content.replace("<head>", "<head><title>BCC - " + subject + "</title>");
            String allAddresses = String.join("</li><li>", bccAddresses);
            content = content.replace(
                    "<div class=\"content\">", "<div class=\"content\"><ul><li>" + allAddresses + "</li></ul>");
            Files.writeString(mailFile.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        } else {
            Session session = Session.getInstance(getProps(), new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            jakarta.mail.Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(RecipientType.TO, InternetAddress.parse(username));
            String bccList = String.join(",", bccAddresses);
            message.setRecipients(RecipientType.BCC, InternetAddress.parse(bccList));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=UTF-8");

            executor.schedule(
                    () -> {
                        try {
                            Transport.send(message);
                        } catch (Exception e) {
                            logger.error(e.toString());
                        }
                    },
                    delay,
                    unit);
        }
    }
}
