package im.conversations.up;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import dev.paseto.jpaseto.Pasetos;
import dev.paseto.jpaseto.lang.Keys;
import im.conversations.up.configuration.Configuration;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.addr.Jid;

public class RegistrationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationProvider.class);

    private final SecretKey secretKey;
    private final String uriTemplate;

    public RegistrationProvider(final Configuration.Registration configuration) {
        this.secretKey = Keys.secretKey(configuration.getSharedSecret());
        this.uriTemplate = configuration.getUri();
    }

    public Registration register(final Jid owner, final String application, final String instance) {
        Preconditions.checkArgument(owner != null, "user must be set");
        checkSha256(application, "application");
        checkSha256(instance, "instance");
        final Instant expiration = Instant.now().plus(Duration.ofDays(7));
        final String token =
                Pasetos.V2
                        .LOCAL
                        .builder()
                        .setSharedSecret(this.secretKey)
                        .setSubject(owner.toEscapedString())
                        .claim("exp", expiration.toEpochMilli())
                        .claim("application", application)
                        .claim("instance", instance)
                        .compact();
        LOGGER.info("{} has registered for push with {}/{}", owner, application, instance);
        final String endpoint =
                UriTemplate.fromTemplate(this.uriTemplate).set("token", token).expand();
        return ImmutableRegistration.builder().expiration(expiration).endpoint(endpoint).build();
    }

    public Target retrieveTarget(final String token) {
        final var claims =
                Pasetos.parserBuilder()
                        .setSharedSecret(this.secretKey)
                        .build()
                        .parse(token)
                        .getClaims();
        return ImmutableTarget.builder()
                .owner(Jid.of(claims.getSubject()))
                .instance(claims.get("instance", String.class))
                .application(claims.get("application", String.class))
                .build();
    }

    private void checkSha256(final String value, final String name) {
        if (value == null) {
            throw new IllegalArgumentException(String.format("%s must be set", name));
        }
        if (BaseEncoding.base64().canDecode(value)) {
            final byte[] decoded = BaseEncoding.base64().decode(value);
            if (decoded.length != 32) {
                throw new IllegalArgumentException(
                        String.format(
                                "%s must be 32 bytes of base64. was %d", name, decoded.length));
            }
        } else {
            throw new IllegalArgumentException(String.format("%s must be valid base64", name));
        }
    }
}
