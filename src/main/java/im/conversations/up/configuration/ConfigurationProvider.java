package im.conversations.up.configuration;

import im.conversations.up.Services;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationProvider {

    public static Configuration readFile(final Path path) throws IOException {
        return Services.GSON.fromJson(
                Files.newBufferedReader(path, StandardCharsets.UTF_8), Configuration.class);
    }
}
