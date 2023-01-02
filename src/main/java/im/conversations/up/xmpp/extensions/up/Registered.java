package im.conversations.up.xmpp.extensions.up;

import java.time.Instant;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import rocks.xmpp.util.adapters.InstantAdapter;

@XmlRootElement(name = "registered")
public class Registered {

    @XmlAttribute private final String endpoint;

    @XmlAttribute
    @XmlJavaTypeAdapter(InstantAdapter.class)
    private final Instant expiration;

    public Registered() {
        this.endpoint = null;
        this.expiration = null;
    }

    public Registered(final String endpoint, final Instant expires) {
        this.endpoint = endpoint;
        this.expiration = expires;
    }
}
