package im.conversations.up;

import static java.util.stream.Collectors.*;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import im.conversations.up.configuration.ImmutableRegistration;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rocks.xmpp.addr.Jid;

public class RegistrationProviderTest {

    @Test
    public void endpoint() throws URISyntaxException {
        final var owner = Jid.of("test@example.com");
        final var application = hash("eu.siacs.conversations");
        final var instance = hash("random");
        final var registrationProvider =
                new RegistrationProvider(
                        ImmutableRegistration.builder()
                                .uri("https://up.conversations.im/push{?token}")
                                .sharedSecret(
                                        BaseEncoding.base64()
                                                .decode(
                                                        "7z8XLjkj/8h2T22nd1Z279vT2ONMOgQu8xj0BLps3qA="))
                                .build());
        final var registration = registrationProvider.register(owner, application, instance);
        final var uri = new URI(registration.getEndpoint());

        final var parameters =
                Pattern.compile("&")
                        .splitAsStream(uri.getQuery())
                        .map(s -> Arrays.copyOf(s.split("=", 2), 2))
                        .collect(
                                groupingBy(
                                        s -> decode(s[0]), mapping(s -> decode(s[1]), toList())));

        final var token = Iterables.getOnlyElement(parameters.get("token"));

        final var target = registrationProvider.retrieveTarget(token);

        Assertions.assertEquals(application, target.getApplication());
        Assertions.assertEquals(instance, target.getInstance());
        Assertions.assertEquals(owner, target.getOwner());
    }

    private static String decode(final String encoded) {
        return Optional.ofNullable(encoded)
                .map(e -> URLDecoder.decode(e, StandardCharsets.UTF_8))
                .orElse(null);
    }

    private String hash(final String in) {
        return BaseEncoding.base64()
                .encode(Hashing.sha256().hashString(in, StandardCharsets.UTF_8).asBytes());
    }
}
