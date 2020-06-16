
package tk.sherrao.discord.titanbash;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.SubscribeEvent;

public class ChatSnooper implements EventListener, Runnable {

    private final Object lock = new Object();
    private final Bot bot;
    
    private Map<String, List<String>> log;
    private SimpleDateFormat fileFormatter;
    private SimpleDateFormat timestampFormatter;

    private String login;
    private String password;
    private String receipient;
    
    private Properties properties;
    private Authenticator authenticator;
    private Session session;
    private Message email;

    public ChatSnooper( final Bot bot ) {
        this.bot = bot;
        
        this.log = Collections.synchronizedMap( new HashMap<String, List<String>>() );
        this.fileFormatter = new SimpleDateFormat( "dd-MM-yyyy_HH-mm-ss" );
        this.timestampFormatter = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss" );
        
        this.login = "";
        this.password = "";
        this.receipient = "";
        
        this.properties = new Properties();
        properties.put( "mail.smtp.auth", "true" );
        properties.put( "mail.smtp.starttls.enable", "true" );
        properties.put( "mail.smtp.host", "smtp.gmail.com" );
        properties.put( "mail.smtp.port", "587" );
        
        this.authenticator = new Authenticator() {
            
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication( login, password );

            }
            
        };
        
        try {
            this.session = Session.getInstance( properties, authenticator );
            this.email = new MimeMessage( session );
            email.setFrom( new InternetAddress( login ) );
            email.addRecipients( RecipientType.TO, InternetAddress.parse( receipient ) );

        } catch ( MessagingException e ) { 
            e.printStackTrace(); 
            
        }

    }

    @SubscribeEvent
    public void onMemberChat( GuildMessageReceivedEvent event ) {
        synchronized ( lock ) {
            String user = event.getAuthor().getName();
            String channel = event.getChannel().getName();
            String message = event.getMessage().getContentRaw();
            String date = timestampFormatter.format( new Date() );
            
            List<String> list = log.getOrDefault( channel, new ArrayList<String>() );
            list.add( new StringBuilder( "[" )
                    .append( date )
                    .append( "] [" )
                    .append( user )
                    .append( "]: " )
                    .append( message )
                    .toString() );
             
            log.put( channel, list );
            
        }
    }

    @Override
    public void run() {
        synchronized ( lock ) {
            try {
                Date now = new Date();
                Multipart contents = new MimeMultipart();

                for( Entry<String, List<String>> entry : log.entrySet() ) {
                    String channel = entry.getKey();
                    List<String> text = entry.getValue();
                    
                    File file = new File( channel + "-" + fileFormatter.format( now ) + ".txt" );
                    file.createNewFile();
                    
                    FileWriter out = new FileWriter( file );
                    for( String line : text )
                        out.append( line + "\n" );
                    
                    out.close();
                    
                    BodyPart attachment = new MimeBodyPart();
                    DataSource source = new FileDataSource( file.getAbsoluteFile() );  
                    attachment.setDataHandler( new DataHandler( source ) );  
                    attachment.setFileName( file.getName() );  
                    contents.addBodyPart( attachment );
                    file.delete();
                    
                }
                
                email.setSubject( "Discord Chat Logs - " + timestampFormatter.format( now ) );
                email.setContent( contents );
                Transport.send( email );
                
                bot.getLog().info( "Sent chatlog email: '" + email.getSubject() + "'" );

            } catch( IOException | MessagingException e ) {
                e.printStackTrace();
                
            } 
        }
    }
    
}