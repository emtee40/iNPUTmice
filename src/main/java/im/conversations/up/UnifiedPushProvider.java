package im.conversations.up;

import im.conversations.up.configuration.Configuration;
import im.conversations.up.configuration.ConfigurationProvider;
import im.conversations.up.web.WebServer;
import im.conversations.up.xmpp.TransportComponent;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class UnifiedPushProvider implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedPushProvider.class);

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
            LOGGER.error(
                    "Configuration file {} does not exist", configurationFile.toAbsolutePath());
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
