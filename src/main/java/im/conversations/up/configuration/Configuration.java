package im.conversations.up.configuration;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import rocks.xmpp.addr.Jid;

@Value.Immutable
@Gson.TypeAdapters
public interface Configuration {

    Component getComponent();

    Registration getRegistration();

    WebServer getWebServer();

    @Value.Immutable
    interface Component {
        String getHostname();

        int getPort();

        Jid getJid();

        String getSharedSecret();
    }

    @Value.Immutable
    interface Registration {

        byte[] getSharedSecret();

        String getUri();
    }

    @Value.Immutable
    interface WebServer {
        String getIp();

        int getPort();
    }
}
