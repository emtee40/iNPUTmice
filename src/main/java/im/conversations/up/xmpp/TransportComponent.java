package im.conversations.up.xmpp;

import com.google.common.base.Throwables;
import im.conversations.up.Registration;
import im.conversations.up.RegistrationProvider;
import im.conversations.up.Target;
import im.conversations.up.configuration.Configuration;
import im.conversations.up.xmpp.extensions.up.Push;
import im.conversations.up.xmpp.extensions.up.Register;
import im.conversations.up.xmpp.extensions.up.Registered;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.Extension;
import rocks.xmpp.core.session.XmppSessionConfiguration;
import rocks.xmpp.core.stanza.AbstractIQHandler;
import rocks.xmpp.core.stanza.IQHandler;
import rocks.xmpp.core.stanza.model.IQ;
import rocks.xmpp.core.stanza.model.StanzaErrorException;
import rocks.xmpp.core.stanza.model.errors.Condition;
import rocks.xmpp.extensions.component.accept.ExternalComponent;
import rocks.xmpp.extensions.muc.model.Muc;

public final class TransportComponent implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportComponent.class);

    private final ExternalComponent component;

    public TransportComponent(final Configuration.Component configuration) {
        this.component = of(configuration);
    }

    private static ExternalComponent of(final Configuration.Component configuration) {
        final var sessionConfiguration =
                XmppSessionConfiguration.builder()
                        .extensions(Extension.of(Register.class, Registered.class, Push.class))
                        .build();
        final var component =
                ExternalComponent.create(
                        configuration.getJid().toEscapedString(),
                        configuration.getSharedSecret(),
                        sessionConfiguration,
                        configuration.getHostname(),
                        configuration.getPort());
        BabblerFixes.apply(component);
        component.disableFeature(Muc.NAMESPACE);
        return component;
    }

    public void setRegistrationProvider(final RegistrationProvider registrationProvider) {
        final IQHandler registerHandler =
                new AbstractIQHandler(Register.class, IQ.Type.SET) {
                    @Override
                    protected IQ processRequest(final IQ iq) {
                        final var request = iq.getExtension(Register.class);
                        final var application = request == null ? null : request.getApplication();
                        final var instance = request == null ? null : request.getInstance();
                        final Registration registration;
                        try {
                            registration =
                                    registrationProvider.register(
                                            iq.getFrom(), application, instance);
                        } catch (final IllegalArgumentException e) {
                            return iq.createError(Condition.BAD_REQUEST);
                        } catch (final Exception e) {
                            LOGGER.error("Could not register {} for push", iq.getFrom(), e);
                            return iq.createError(Condition.INTERNAL_SERVER_ERROR);
                        }
                        return iq.createResult(
                                new Registered(
                                        registration.getEndpoint(), registration.getExpiration()));
                    }
                };
        this.component.addIQHandler(registerHandler);
    }

    public UUID sendPushMessage(final Target target, final byte[] payload) {
        LOGGER.info(
                "pushing {} bytes to {}/{} of {}",
                payload.length,
                target.getApplication(),
                target.getInstance(),
                target.getOwner());
        final UUID uuid = UUID.randomUUID();
        final IQ push =
                IQ.set(
                        target.getOwner(),
                        new Push(target.getApplication(), target.getInstance(), payload));
        push.setId(uuid.toString());
        this.component
                .query(push)
                .handle(
                        (result, throwable) -> {
                            if (throwable != null) {
                                onPushMessageFailure(target, Throwables.getRootCause(throwable));
                            }
                            return null;
                        });
        return uuid;
    }

    private void onPushMessageFailure(final Target target, final Throwable throwable) {
        if (throwable instanceof StanzaErrorException) {
            final StanzaErrorException stanzaErrorException = (StanzaErrorException) throwable;
            if (Condition.ITEM_NOT_FOUND.equals(stanzaErrorException.getCondition())) {
                LOGGER.warn("instance {} of {} is gone", target.getInstance(), target.getOwner());
                // TODO add instance to revocation list until it expires
                return;
            }
        }
        LOGGER.info(
                "ignoring temporary error for {} of {}", target.getInstance(), target.getOwner());
    }

    public void connectAndRetry() throws InterruptedException {
        while (true) {
            try {
                this.component.connect();
                while (this.component.isConnected()) {
                    Thread.sleep(500);
                }
            } catch (final XmppException e) {
                System.err.println(e.getMessage());
            }
            Thread.sleep(2000);
        }
    }

    @Override
    public void close() throws Exception {
        this.component.close();
    }
}
