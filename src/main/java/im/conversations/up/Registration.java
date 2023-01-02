package im.conversations.up;

import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
public interface Registration {

    String getEndpoint();

    Instant getExpiration();
}
