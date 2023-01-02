package im.conversations.up.xmpp.extensions.up;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "push")
public class Push {

    @XmlAttribute private final String application;

    @XmlAttribute private final String instance;

    @XmlValue private final byte[] payload;

    public Push() {
        this.application = null;
        this.instance = null;
        this.payload = null;
    }

    public Push(final String application, final String instance, final byte[] payload) {
        this.application = application;
        this.instance = instance;
        this.payload = payload;
    }
}
