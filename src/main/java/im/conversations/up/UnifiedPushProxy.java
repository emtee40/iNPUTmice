package im.conversations.up;

import im.conversations.up.configuration.Configuration;
import im.conversations.up.configuration.ConfigurationProvider;
import im.conversations.up.web.WebServer;
import im.conversations.up.xmpp.TransportComponent;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;

public class UnifiedPushProxy implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-c", "--config"},
            required = true)
    private Path configurationFile;

    @Override
    public Integer call() throws Exception {
        final Configuration configuration;
        try {
            configuration = ConfigurationProvider.readFile(configurationFile);
        } catch (final NoSuchFileException e) {
            System.err.printf(
                    "Configuration file %s does not exist%n", configurationFile.toAbsolutePath());
            return 2;
        }
        return execute(configuration);
    }

    private Integer execute(final Configuration configuration) throws Exception {
        final var registrationProvider = new RegistrationProvider(configuration.getRegistration());
        final var webServer = new WebServer(configuration.getWebServer());
        webServer.setRegistrationProvider(registrationProvider);
        try (final var transportComponent = new TransportComponent(configuration.getComponent())) {
            transportComponent.setRegistrationProvider(registrationProvider);
            webServer.setOnPushMessage(transportComponent::sendPushMessage);
            transportComponent.connectAndRetry();
        }
        return 0;
    }
}
