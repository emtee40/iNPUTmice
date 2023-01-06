package im.conversations.up.web;

import com.damnhandy.uri.template.UriTemplate;
import dev.paseto.jpaseto.ExpiredPasetoException;
import dev.paseto.jpaseto.PasetoSecurityException;
import im.conversations.up.RegistrationProvider;
import im.conversations.up.Target;
import im.conversations.up.configuration.Configuration;
import java.util.UUID;
import spark.Request;
import spark.Response;
import spark.Spark;

public class WebServer {

    private static final int MAX_PAYLOAD_SIZE = 4096;

    private final String messageUriTemplate;
    private RegistrationProvider registrationProvider;
    private OnPushMessage onPushMessage;

    public WebServer(final Configuration.WebServer configuration) {
        Spark.ipAddress(configuration.getIp());
        Spark.port(configuration.getPort());
        Spark.exception(
                PasetoSecurityException.class,
                (exception, request, response) -> response.status(404));
        Spark.exception(
                ExpiredPasetoException.class,
                (exception, request, response) -> response.status(404));
        Spark.exception(
                PayloadTooLargeException.class,
                ((exception, request, response) -> response.status(413)));
        Spark.post("/push/:token", this::receivePushRequest);
        this.messageUriTemplate = configuration.getMessageUri();
    }

    private String receivePushRequest(final Request request, final Response response) {
        final OnPushMessage callback = this.onPushMessage;
        final RegistrationProvider registrationProvider = this.registrationProvider;
        if (callback == null || registrationProvider == null) {
            throw new IllegalStateException("Server not ready");
        }
        final String token = request.params("token");
        final var length = request.contentLength();
        if (length > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException();
        }
        final var target = registrationProvider.retrieveTarget(token);
        final var payload = request.bodyAsBytes();
        if (payload.length > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException();
        }
        final UUID uuid = callback.onPushMessage(target, payload);
        response.status(201);
        final String location =
                UriTemplate.fromTemplate(this.messageUriTemplate).set("uuid", uuid).expand();
        response.header("Location", location);
        return "";
    }

    public void setRegistrationProvider(final RegistrationProvider registrationProvider) {
        this.registrationProvider = registrationProvider;
    }

    public void setOnPushMessage(final OnPushMessage onPushMessage) {
        this.onPushMessage = onPushMessage;
    }

    public interface OnPushMessage {
        UUID onPushMessage(final Target target, final byte[] payload);
    }

    public static class PayloadTooLargeException extends RuntimeException {}
}
