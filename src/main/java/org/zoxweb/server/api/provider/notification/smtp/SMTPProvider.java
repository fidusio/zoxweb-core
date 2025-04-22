/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.api.provider.notification.smtp;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.zoxweb.server.api.APIServiceProviderBase;
import org.zoxweb.server.http.HTTPUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.api.*;
import org.zoxweb.shared.filters.MessageContentFilter;
import org.zoxweb.shared.util.*;

import java.util.List;
import java.util.Properties;


/**
 * The Simple Mail Transfer Protocol (SMTP) provider class is used an email from the server.
 */
@SuppressWarnings("serial")
public class SMTPProvider
    extends APIServiceProviderBase<Void, Void>
	implements APINotification<Void>
{

	private static final LogWrapper log = new LogWrapper(SMTPProvider.class);
	
	/**
	 * This enum contains SMTP message parameters.
	 */
	public enum SMTPMessageParam 
		implements GetValue<String>
	{
		
		SENDER_ID_NAME("sender_id_name"),
		CC("cc"),
		BCC("bcc"),
		TEXT("text"),
		HTML("html"),
		TEXT_HTML("text/html; charset=UTF-8"),
		MESSAGE_ID("Message-ID")
		
		;

		private String name;
		
		SMTPMessageParam(String name)
		{
			this.name = name;
		}
		
		
		public String getValue() 
		{
			return name;
		}
		
	}
	
	
	class SMTPSenderTask
		implements Runnable
	{
		SMTPProvider smtpProvider;
		APINotificationMessage notificationMessage;

		SMTPSenderTask(SMTPProvider smtpProvider, APINotificationMessage notificationMessage)
		{
			this.smtpProvider = smtpProvider;
			this.notificationMessage = notificationMessage;
		}

		public void  run() 
		{
			//sendAPIMessageInternal((APIMessage) event.getTaskExecutorParameters()[0]);
//			TaskEvent event = attachedEvent();
//			int index = 0;
//			SMTPProvider smtpProvider =  (SMTPProvider) event.getTaskExecutorParameters()[index++];
//			APINotificationMessage notificationMessage = (APINotificationMessage) event.getTaskExecutorParameters()[index++];
			
			
			final String USER_NAME = getAPIConfigInfo().getProperties().getValue(SMTPCreator.Param.USERNAME.getName());
			final String PASSWORD  = getAPIConfigInfo().getProperties().getValue(SMTPCreator.Param.PASSWORD.getName());
			
			
			Session session = smtpProvider.createSession(
					smtpProvider.createProperties(true, true, getAPIConfigInfo().getProperties().getValue(SMTPCreator.Param.HOST.getName()),
					    getAPIConfigInfo().getProperties().getValue(SMTPCreator.Param.PORT.getName())),
					USER_NAME, PASSWORD);

		      try 
		      {
		         // Default MimeMessage object.
		         MimeMessage msg = new MimeMessage(session);
		         
		         // Set From
		         String from = SharedUtil.lookupValue(notificationMessage.getExtraAttribues().get(SMTPMessageParam.SENDER_ID_NAME.getValue()));
		         
		         if (from != null)
		         {
			         from = from + " <" + notificationMessage.getSenderID() + ">";
		         }
		         
		         else 
		         {
			         from = notificationMessage.getSenderID();
		         }
		        	 
		         msg.setFrom(new InternetAddress(from));
		         
		         // Set To
		         StringBuilder sb = new StringBuilder();
		      
		         for (String to : notificationMessage.getRecipientIDs())
		         {
		        	if (sb.length() > 0)
		        		sb.append(",");
		        		
		        	sb.append(to);
		         }
		         
		         msg.addRecipients(Message.RecipientType.TO, sb.toString());
		         
		         //msg.setHeader("Content-Type", "text/plain; charset=UTF-8");
		         // Set Cc
		         @SuppressWarnings("unchecked")
		         List<NVPair> ccList = (List<NVPair>) SharedUtil.lookupArrayValues(notificationMessage.getExtraAttribues(), SMTPMessageParam.CC.getValue());
		         
		         if (ccList != null)
		         {
		        	 for (NVPair nvp : ccList)
		        	 {
		        		 msg.addRecipient(Message.RecipientType.CC, new InternetAddress(nvp.getValue()));
		        	 }
		         }
		         
		         //	Set Bcc
		         @SuppressWarnings("unchecked")
		         List<NVPair> bccList = (List<NVPair>) SharedUtil.lookupArrayValues(notificationMessage.getExtraAttribues(), SMTPMessageParam.BCC.getValue());
		         
		         if (bccList != null)
		         {
		        	 for (NVPair nvp : bccList)
		        	 {
		        		 msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(nvp.getValue()));
		        	 }
		         }
		         
		         // Set Subject
		         msg.setSubject(notificationMessage.getTitle());

		         // Set Message
		         String bodyContent = MessageContentFilter.SINGLETON.validate(notificationMessage);
		         
		         
		         
		         if (HTTPUtil.isHTML(bodyContent))
		         {
		        	 //System.out.println("WE have HTML message\n" + bodyContent);
		        	 msg.setContent(bodyContent, SMTPMessageParam.TEXT_HTML.getValue());
		         }
				 else
		         {
		        	 //System.out.println("WE have TEXT message\b" + bodyContent);
		        	 //msg.setHeader("Content-Type", "text/plain; charset=UTF-8");
		        	 msg.setText(bodyContent, Const.UTF_8);
		         }

		         // Send Message
		         Transport.send(msg);

				 if (log.isEnabled()) log.getLogger().info(SMTPMessageIDFilter.SINGLETON.validate(msg.getHeader(SMTPMessageParam.MESSAGE_ID.getValue())));
		      } 
		      
		      catch (MessagingException e)
		      {
		            throw new RuntimeException(e);
		      }
		}
		
	}
	
	//private APIConfigInfo configInfo;
	private String name;
	private String description;
	private APIExceptionHandler exceptionHandler;
	//private Properties properties;
	//private Session session;


	
	public Void connect() 
			throws APIException
	{
		return null;
	}

	
	public void close() 
			throws APIException 
	{
		
	}

	
	public boolean isProviderActive()
	{
		return false;
	}

	@Override
	public APIExceptionHandler getAPIExceptionHandler() 
	{
		return exceptionHandler;
	}

	@Override
	public void setAPIExceptionHandler(APIExceptionHandler exceptionHandler) 
	{
		this.exceptionHandler = exceptionHandler;
	}


	public void setDescription(String str) 
	{
		description = str;
	}


	public String getDescription() 
	{
		return description;
	}


	public void setName(String name) 
	{
		this.name = name;
	}

	
	public String getName() 
	{
		return name;
	}

	
	public String toCanonicalID() 
	{
		return null;
	}

	
	public APITransactionInfo sendAPIMessage(APIMessage message, APINotificationDelivery apind)
        throws NullPointerException, IllegalArgumentException, APIException
	{
		//		check the message if not null
		//		message must be of type email
		//		if not throw IllegalArgumentException
		//		validate parameters (from, to (at least one recipient), bcc (later), cc (later))
		//		set body
		//		tags (later)
		//		Process tags
		//		Extract APIConfigInfo set them to transport
		//		Support dynamic enum later (hard coded for now).
		//		Set notifications
		
		if (getAPIConfigInfo() == null)
		{
			throw new APIException("Missing configuration information");
		}

		SUS.checkIfNulls("APINotificationMessage is null", message);
		
		if (!(message instanceof APINotificationMessage))
		{
			throw new IllegalArgumentException("Message is not an instance of APINotificationMessage");
		}
		
		if (!getAPIConfigInfo().isServiceTypeSupported(message.getMessageType()))
		{
			throw new IllegalArgumentException("Message is not an email type.");
		}

//		TaskExecutor td = new SMTPSenderTask();
//		TaskEvent    te = new TaskEvent(this, td, this, message);
		SMTPSenderTask smtpSenderTask = new SMTPSenderTask(this, (APINotificationMessage)message);

		switch(apind)
		{
		case NOW:
//			td.executeTask(te);
			//return sendAPIMessageInternal(message);
			smtpSenderTask.run();
			break;
			
		case QUEUED:
			TaskUtil.defaultTaskScheduler().queue(0, smtpSenderTask);
		default:
			break;
		}

		return null;
	}

//	private APITransactionInfo sendAPIMessageInternal(APIMessage message)
//						throws NullPointerException, IllegalArgumentException, APIException
//	{
//		APINotificationMessage notificationMessage = (APINotificationMessage) message;
//		
//		final String USER_NAME = SharedUtil.lookupValue(configInfo.getConfigParameters().get(SMTPCreator.Param.USERNAME.getName()));
//		final String PASSWORD  = SharedUtil.lookupValue(configInfo.getConfigParameters().get(SMTPCreator.Param.PASSWORD.getName()));
//		
//		createProperties(
//				true,
//				true,
//				SharedUtil.lookupValue(configInfo.getConfigParameters().get(SMTPCreator.Param.HOST.getName())),
//				SharedUtil.lookupValue(configInfo.getConfigParameters().get(SMTPCreator.Param.PORT.getName())));
//		
//		 Session session = createSession(createProperties(true, true, SharedUtil.lookupValue(configInfo.getConfigParameters().get(SMTPCreator.Param.HOST.getName())), SharedUtil.lookupValue(configInfo.getConfigParameters().get(SMTPCreator.Param.PORT.getName()))),
//				                         USER_NAME, PASSWORD);
//
//	      try 
//	      {
//	         // Default MimeMessage object.
//	         MimeMessage msg = new MimeMessage(session);
//	         
//	         // Set From
//	         String from = SharedUtil.lookupValue(notificationMessage.getExtraAttribues(), SMTPMessageParam.SENDER_ID_NAME);
//	         
//	         if (from != null)
//	         {
//		         from = from + " <" + notificationMessage.getSenderID() + ">";
//	         }
//	         
//	         else 
//	         {
//		         from = notificationMessage.getSenderID();
//	         }
//	        	 
//	         msg.setFrom(new InternetAddress(from));
//	         
//	         // Set To
//	         StringBuilder sb = new StringBuilder();
//	      
//	         for (String to : notificationMessage.getRecipientIDs())
//	         {
//	        	if (sb.length() > 0)
//	        		sb.append(",");
//	        		
//	        	sb.append(to);
//	         }
//	         
//	         msg.addRecipients(Message.RecipientType.TO, sb.toString());
//	         
//	         //msg.setHeader("Content-Type", "text/plain; charset=UTF-8");
//	         // Set Cc
//	         @SuppressWarnings("unchecked")
//	         List<NVPair> ccList = (List<NVPair>) SharedUtil.lookupAllNV(notificationMessage.getExtraAttribues(), SMTPMessageParam.CC.getValue());
//	         
//	         if (ccList != null)
//	         {
//	        	 for (NVPair nvp : ccList)
//	        	 {
//	        		 msg.addRecipient(Message.RecipientType.CC, new InternetAddress(nvp.getValue()));
//	        	 }
//	         }
//	         
//	         //	Set Bcc
//	         @SuppressWarnings("unchecked")
//	         List<NVPair> bccList = (List<NVPair>) SharedUtil.lookupAllNV(notificationMessage.getExtraAttribues(), SMTPMessageParam.BCC.getValue());
//	         
//	         if (bccList != null)
//	         {
//	        	 for (NVPair nvp : bccList)
//	        	 {
//	        		 msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(nvp.getValue()));
//	        	 }
//	         }
//	         
//	         // Set Subject
//	         msg.setSubject(notificationMessage.getTitle());
//
//	         // Set Message
//	         String bodyContent = MessageContentFilter.SINGLETON.validate(notificationMessage);
//	         
//	         if (HTMLFilter.isHtml(bodyContent))
//	         {
//	        	 
//	        	 msg.setContent(bodyContent, SMTPMessageParam.TEXT_HTML.getValue());
//	         }
//	         
//	         else
//	         { 
//	        	 //msg.setHeader("Content-Type", "text/plain; charset=UTF-8");
//	        	 msg.setText(bodyContent, "utf-8");
//	         }
//	         
//	         
//	         
//	         // Send Message
//	         Transport.send(msg);
//	         
//	         
//	        // System.out.println(SMTPMessageIDFilter.SINGLETON.validate(msg.getHeader("Message-ID")));
//	      } 
//	      
//	      catch (MessagingException e) 
//	      {
//	            throw new RuntimeException(e);
//	      }
//		
//		
//		return null;
//	}
	
	/**
	 * 
	 * @param authentication
	 * @param encrypt
	 * @param host
	 * @param port
	 */
	private Properties  createProperties(boolean authentication, boolean encrypt, String host, String port)
	{		
	      Properties properties = new Properties();
	      properties.put("mail.smtp.auth", "" + authentication);
	      properties.put("mail.smtp.ssl.enable", "" + encrypt);
	      properties.put("mail.smtp.host", host);
	      properties.put("mail.smtp.port", port);
	      
	      return properties;
	}

	/**
	 * 
	 * @param properties
	 * @param userName
	 * @param password
	 */
	private Session createSession(Properties properties, final String userName, final String password)
	{
	   Session session = Session.getInstance(properties, 
			   new Authenticator()
	   			{@Override
			       protected PasswordAuthentication getPasswordAuthentication()
			       {
			          return new PasswordAuthentication(userName, password);
			       }
	   			});
	   
	   return session;
	}


	public APITransactionInfo updateTransactionInfo(APITransactionInfo transaction) 
        throws NullPointerException, IllegalArgumentException, APIException
	{

		return null;
	}

	
	public Void newConnection()
        throws APIException
	{
		return null;
	}

	@Override
	public <T> T lookupProperty(GetName propertyName)
	{
		return null;
	}

}
