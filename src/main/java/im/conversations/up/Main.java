package im.conversations.up;

import picocli.CommandLine;

public class Main {
    public static void main(final String... args) {
        final var exitCode = new CommandLine(new UnifiedPushProxy()).execute(args);
        System.exit(exitCode);
    }
}
