package lab.web.mail;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SMTPAuth extends Authenticator{
	
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication("kpunh1542@gmail.com", "qkrgkdms27!");
	}
	
	public static boolean sendEmail(String from, String name, String subject, String content) {
		Properties pro = new Properties();
		pro.put("mail.smtp.host", "smtp.gmail.com");
		pro.put("mail.smtp.port", "465");
		pro.put("mail.smtp.auth", "true");
		pro.put("mail.smtp.ssl.enable", "true");
		pro.put("mail.smtp.starttls.enable","true");
		try {
			SMTPAuth auth = new SMTPAuth();
			Session session = Session.getInstance(pro, auth);
			MimeMessage msg = new MimeMessage(session);
			msg.setSubject(subject);
			Address froms = new InternetAddress(from, name);
			msg.setFrom(froms);
			Address tos = new InternetAddress("kpunh1542@gmail.com");
			msg.addRecipient(Message.RecipientType.TO, tos);
			msg.setContent("보내는 분 : " + from + 
					"<br>성함 : " + name + 
					"<br>" + content, 
					"text/html; charset=UTF-8");
			Transport.send(msg);
		} catch(AddressException e){
			e.printStackTrace();
			throw new RuntimeException("주소가 잘못 되었습니다. 콘솔확인");
		} catch(MessagingException | UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException("메일을 보내는 과정에서 문제가 생겼습니다. 콘솔확인");
		}
		return true;
	}
	

}
