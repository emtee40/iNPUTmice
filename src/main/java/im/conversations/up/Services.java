package im.conversations.up;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import im.conversations.up.configuration.GsonAdaptersConfiguration;
import java.io.IOException;
import rocks.xmpp.addr.Jid;

public final class Services {

    public static final Gson GSON =
            new GsonBuilder()
                    .registerTypeAdapterFactory(new GsonAdaptersConfiguration())
                    .registerTypeAdapter(Jid.class, new JidTypeAdapter())
                    .registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter())
                    .create();

    private Services() {}

    private static class JidTypeAdapter extends TypeAdapter<Jid> {

        @Override
        public void write(final JsonWriter writer, final Jid value) throws IOException {
            if (value == null) {
                writer.nullValue();
            } else {
                writer.value(value.toEscapedString());
            }
        }

        @Override
        public Jid read(final JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                return null;
            } else if (reader.peek() == JsonToken.STRING) {
                final String value = reader.nextString();
                return Jid.of(value);
            } else {
                throw new JsonParseException(
                        String.format(
                                "Expected string or null to read jid. Was  %s",
                                reader.peek().name()));
            }
        }
    }

    private static class ByteArrayTypeAdapter extends TypeAdapter<byte[]> {

        @Override
        public void write(final JsonWriter writer, byte[] value) throws IOException {
            if (value == null) {
                writer.nullValue();
            } else {
                writer.value(BaseEncoding.base64().encode(value));
            }
        }

        @Override
        public byte[] read(final JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                return null;
            } else if (reader.peek() == JsonToken.STRING) {
                final String value = reader.nextString();
                return BaseEncoding.base64().decode(value);
            } else {
                throw new JsonParseException(
                        String.format(
                                "Expected string or null to read jid. Was  %s",
                                reader.peek().name()));
            }
        }
    }
}
