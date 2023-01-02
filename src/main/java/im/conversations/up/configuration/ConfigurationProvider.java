package im.conversations.up.configuration;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import rocks.xmpp.addr.Jid;

public class ConfigurationProvider {

    public static Configuration readFile(final Path path) throws IOException {
        final Gson gson =
                new GsonBuilder()
                        .registerTypeAdapterFactory(new GsonAdaptersConfiguration())
                        .registerTypeAdapter(Jid.class, new JidTypeAdapter())
                        .registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter())
                        .create();
        return gson.fromJson(
                Files.newBufferedReader(path, StandardCharsets.UTF_8), Configuration.class);
    }

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
