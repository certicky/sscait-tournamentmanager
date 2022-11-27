package server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class EmailUtil {
	 
    /**
     * Utility method to send simple HTML email
     * @param session
     * @param toEmail
     * @param subject
     * @param body
     */
    private static void sendEmail(Session session, String toEmail, String subject, String body, String fromEmail){
        try
        {
          MimeMessage msg = new MimeMessage(session);
          //set message headers
          msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
          msg.addHeader("format", "flowed");
          msg.addHeader("Content-Transfer-Encoding", "8bit");
          msg.setFrom(new InternetAddress(fromEmail, "SSCAIT Server"));
          msg.setReplyTo(InternetAddress.parse(fromEmail, false));
          msg.setSubject(subject, "UTF-8");
          msg.setContent(body, "text/html; charset=utf-8");
          msg.setSentDate(new Date());
          msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
          Transport.send(msg); 
        }
        catch (Exception e) {
          e.printStackTrace();
        }
    }
    
    public static void sendExceptionViaGmail(Throwable exception) {
    	StringWriter sw = new StringWriter();
    	exception.printStackTrace(new PrintWriter(sw));
    	String exceptionAsString = sw.toString().replace("\n", "<br>");
    	
    	if (!ServerSettings.Instance().GmailFromEmail.equals("default@gmail.com")) {
    		sendEmailViaGmail(
    			ServerSettings.Instance().GmailFromEmail, 
    			ServerSettings.Instance().AdminEmail, 
    			ServerSettings.Instance().GmailEmailPassword, 
    			"SSCAIT: Server Exception",
    			exception.getMessage()+"\n\n"+exceptionAsString
    			);
    	}
    }
    
    public static void sendEmailViaGmail(String fromEmail, String toEmail, String password, String subject, String body) {
    	final String sFromEmail = fromEmail; //requires valid gmail id
        final String sPassword = password; // correct password for gmail id
        final String sToEmail = toEmail; // can be any email id
        System.out.println("Sending email from "+sFromEmail+" to "+sToEmail+": "+subject);
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
        props.put("mail.smtp.port", "587"); //TLS Port
        props.put("mail.smtp.auth", "true"); //enable authentication
        props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS
        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sFromEmail, sPassword);
            }
        };
        Session session = Session.getInstance(props, auth);
        sendEmail(session, sToEmail, subject, body, sFromEmail);
    }
    
    
}