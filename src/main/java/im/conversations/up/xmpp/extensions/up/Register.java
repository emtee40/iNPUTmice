package im.conversations.up.xmpp.extensions.up;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "register")
public class Register {

    @XmlAttribute private final String application;

    @XmlAttribute private final String instance;

    public Register() {
        this.application = null;
        this.instance = null;
    }

    public Register(final String application, final String instance) {
        this.application = application;
        this.instance = instance;
    }

    public String getApplication() {
        return this.application;
    }

    public String getInstance() {
        return this.instance;
    }
}
