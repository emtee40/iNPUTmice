package im.conversations.up.xmpp;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.session.XmppSession;
import rocks.xmpp.core.stanza.model.Presence;
import rocks.xmpp.im.subscription.PresenceManager;

public final class BabblerFixes {

    private static final String FIELD_NAME_PRESENCE_MAP = "presenceMap";
    private static final String FIELD_NAME_LAST_SENT_PRESENCES = "lastSentPresences";

    private static final Logger LOGGER = LoggerFactory.getLogger(BabblerFixes.class);

    private BabblerFixes() {}

    public static void apply(final XmppSession session) {
        disableCaching(session.getManager(PresenceManager.class));
    }

    /**
     * Unfortunately PresenceManager stores all incoming and outgoing Presences. Blabber doesn't
     * provide an option to disable that behaviour. Therefore, we replace the HashMaps that store
     * the Presences with a custom implementation of Map that doesn't store anything.
     *
     * @param presenceManager The PresenceManager retrieved from XmppClient
     */
    private static void disableCaching(final PresenceManager presenceManager) {
        try {
            final Field presenceMapField =
                    PresenceManager.class.getDeclaredField(FIELD_NAME_PRESENCE_MAP);
            presenceMapField.setAccessible(true);
            final Field lastSentPresencesField =
                    PresenceManager.class.getDeclaredField(FIELD_NAME_LAST_SENT_PRESENCES);
            lastSentPresencesField.setAccessible(true);
            final Map<Jid, Map<String, Presence>> presenceMap = new NoStoreMap<>();
            final Map<String, Presence> lastSentPresences = new NoStoreMap<>();
            presenceMapField.set(presenceManager, presenceMap);
            lastSentPresencesField.set(presenceManager, lastSentPresences);
        } catch (final Exception e) {
            LOGGER.error("Failed to apply fixes to {}", PresenceManager.class.getSimpleName());
        }
    }

    private static final class NoStoreMap<K, V> implements Map<K, V> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object o) {
            return false;
        }

        @Override
        public boolean containsValue(Object o) {
            return false;
        }

        @Override
        public V get(Object o) {
            return null;
        }

        @Override
        public V put(K k, V v) {
            return null;
        }

        @Override
        public V remove(Object o) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {}

        @Override
        public void clear() {}

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return null;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return null;
        }
    }
}
