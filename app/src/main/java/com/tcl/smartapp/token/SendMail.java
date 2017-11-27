package com.tcl.smartapp.token;

/**
 * Created by user on 5/24/16.
 */
import java.util.Calendar;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMail {
    private static String smtpHost = "smtp.163.com";
    private static String from = "13824208018@163.com";
    private static String fromUserPassword = "557485";
    private static String messageType = "text/html;charset=gb2312";
    public static void sendMessage(String to, String subject,
                                   String messageText) throws MessagingException {
        // 第一步：配置javax.mail.Session对象
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.starttls.enable", "true");//使用 STARTTLS安全连接
        //props.put("mail.smtp.port", "25");             //google使用465或587端口
        props.put("mail.smtp.auth", "true");        // 使用验证
        Session mailSession = Session.getInstance(props, new MyAuthenticator(from, fromUserPassword));
        // 第二步：编写消息
        InternetAddress fromAddress = new InternetAddress(from);
        InternetAddress toAddress = new InternetAddress(to);
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(fromAddress);
        message.addRecipient(RecipientType.TO, toAddress);
        message.setSentDate(Calendar.getInstance().getTime());
        message.setSubject(subject);
        message.setContent(messageText, messageType);
        // 第三步：发送消息
        Transport transport = mailSession.getTransport("smtp");
        transport.connect(smtpHost, "13824208018", fromUserPassword);
        transport.send(message, message.getRecipients(RecipientType.TO));
    }
}
