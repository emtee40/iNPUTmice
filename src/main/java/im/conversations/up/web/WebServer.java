package im.conversations.up.web;

import im.conversations.up.RegistrationProvider;
import im.conversations.up.Target;
import im.conversations.up.configuration.Configuration;
import spark.Request;
import spark.Response;
import spark.Spark;

public class WebServer {

    private static final int MAX_PAYLOAD_SIZE = 4096;

    private RegistrationProvider registrationProvider;
    private OnPushMessage onPushMessage;

    public WebServer(final Configuration.WebServer configuration) {
        Spark.ipAddress(configuration.getIp());
        Spark.port(configuration.getPort());
        Spark.post("/push", this::receivePushRequest);
    }

    private String receivePushRequest(final Request request, final Response response) {
        final OnPushMessage callback = this.onPushMessage;
        final RegistrationProvider registrationProvider = this.registrationProvider;
        if (callback == null || registrationProvider == null) {
            throw new IllegalStateException("Server not ready");
        }
        final String token = request.queryParams("token");
        final var length = request.contentLength();
        if (length > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException();
        }
        final var target = registrationProvider.retrieveTarget(token);
        final var payload = request.bodyAsBytes();
        if (payload.length > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException();
        }
        callback.onPushMessage(target, payload);
        return "";
    }

    public void setRegistrationProvider(final RegistrationProvider registrationProvider) {
        this.registrationProvider = registrationProvider;
    }

    public void setOnPushMessage(final OnPushMessage onPushMessage) {
        this.onPushMessage = onPushMessage;
    }

    public interface OnPushMessage {
        void onPushMessage(final Target target, final byte[] payload);
    }

    public static class PayloadTooLargeException extends RuntimeException {}
}
