package im.conversations.up.xmpp;

import im.conversations.up.Registration;
import im.conversations.up.RegistrationProvider;
import im.conversations.up.configuration.Configuration;
import im.conversations.up.xmpp.extensions.up.Register;
import im.conversations.up.xmpp.extensions.up.Registered;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.Extension;
import rocks.xmpp.core.session.XmppSessionConfiguration;
import rocks.xmpp.core.stanza.AbstractIQHandler;
import rocks.xmpp.core.stanza.IQHandler;
import rocks.xmpp.core.stanza.model.IQ;
import rocks.xmpp.core.stanza.model.errors.Condition;
import rocks.xmpp.extensions.component.accept.ExternalComponent;
import rocks.xmpp.extensions.muc.model.Muc;

public final class ExternalComponents {

    private ExternalComponents() {}

    public static ExternalComponent of(final Configuration.Component configuration) {
        final var sessionConfiguration =
                XmppSessionConfiguration.builder()
                        .extensions(Extension.of(Register.class, Registered.class))
                        .build();
        final var component =
                ExternalComponent.create(
                        configuration.getJid().toEscapedString(),
                        configuration.getSharedSecret(),
                        sessionConfiguration,
                        configuration.getHostname(),
                        configuration.getPort());
        component.disableFeature(Muc.NAMESPACE);
        return component;
    }

    public static void setRegistrationProvider(
            final ExternalComponent component, final RegistrationProvider registrationProvider) {
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
                        } catch (final IllegalStateException e) {
                            return iq.createError(Condition.INTERNAL_SERVER_ERROR);
                        }
                        return iq.createResult(
                                new Registered(
                                        registration.getEndpoint(), registration.getExpiration()));
                    }
                };
        component.addIQHandler(registerHandler);
    }

    public static void connectAndRetry(final ExternalComponent component)
            throws InterruptedException {
        while (true) {
            try {
                component.connect();
                while (component.isConnected()) {
                    Thread.sleep(500);
                }
            } catch (final XmppException e) {
                System.err.println(e.getMessage());
            }
            Thread.sleep(2000);
        }
    }
}
