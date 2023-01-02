package im.conversations.up;

import org.immutables.value.Value;
import rocks.xmpp.addr.Jid;

@Value.Immutable
public interface Target {

    Jid getOwner();

    String getApplication();

    String getInstance();
}
