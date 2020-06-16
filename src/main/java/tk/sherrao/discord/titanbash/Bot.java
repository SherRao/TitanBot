package tk.sherrao.discord.titanbash;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Game.GameType;
import net.dv8tion.jda.core.hooks.AnnotatedEventManager;
import tk.sherrao.logging.Logger;

public class Bot {

    private final Logger log;
    private JDA api;
    private ChatSnooper chat;
    private ScheduledExecutorService executor;
    
    public Bot() {
        this.log = new Logger( "Dominate Network Bot" );
        try {
            this.api = new JDABuilder( AccountType.BOT )
                    .setAudioEnabled( false )
                    .setToken( BotInformation.DISCORD_TOKEN )
                    .setGame( Game.of( GameType.DEFAULT, "titanbash.us" ) )
                    .build();
            this.chat = new ChatSnooper( this );
            
            api.awaitReady();
            api.setEventManager( new AnnotatedEventManager() );
            api.addEventListener( chat );
            
        } catch ( LoginException e ) {
            log.error( "Failed to connect to Discord authentication servers. The internet connection on this machine, or Discord might be down at the moment", e );
            System.exit( 0 );
            
        } catch( InterruptedException e ) {
            log.error( "Failed to load the Discord JDA API!", e );
            System.exit( 0 );
            
        }
        
        this.executor = Executors.newScheduledThreadPool( 3 );
        executor.scheduleWithFixedDelay( chat, 1L, 1L, TimeUnit.MINUTES );

    }
    
    public Logger getLog() {
        return log;
        
    }
    
    public JDA getApi() {
        return api;
        
    }
    
}
