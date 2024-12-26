package io.github.gaming32.awremap;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: java -jar awremap.jar <mappings> <input> <output>");
            System.exit(1);
        }

        var start = System.nanoTime();
        final var mappings = loadMappings(Path.of(args[0]));
        System.out.println("\nLoaded " + mappings.getClasses().size() + " class mappings in " + formatSince(start));

        start = System.nanoTime();
        final long count;
        try (
            var reader = createLoggingReader(Path.of(args[1]), "Remapping...");
            var writer = Files.newBufferedWriter(Path.of(args[2]))
        ) {
            count = new AwRemapper(mappings, reader, writer).remap();
        }
        System.out.println("\nRemapped " + count + " entries in " + formatSince(start));
    }

    private static MappingTree loadMappings(Path mappingsPath) throws IOException {
        final var message = "Loading mappings...";
        final var result = new MemoryMappingTree();
        if (Files.isDirectory(mappingsPath)) {
            System.out.println(message);
            MappingReader.read(mappingsPath, result);
            return result;
        }
        try (final var reader = createLoggingReader(mappingsPath, message)) {
            MappingReader.read(reader, result);
        }
        return result;
    }

    private static Reader createLoggingReader(Path file, String message) throws IOException {
        final var size = Files.size(file);
        final var percent = new AtomicInteger();
        System.out.print(message + " 0%\r");
        return new InputStreamReader(new CallbackInputStream(Files.newInputStream(file), read -> {
            final var newPercent = (int) (read * 100 / size);
            if (newPercent != percent.getPlain()) {
                percent.setPlain(newPercent);
                System.out.print(message + ' ' + newPercent + "%\r");
            }
        }));
    }

    private static String formatSince(long startNanos) {
        return formatDuration(Duration.ofNanos(System.nanoTime() - startNanos));
    }

    private static String formatDuration(Duration duration) {
        if (duration.toMillis() < 1000) {
            return duration.toMillis() + "ms";
        }

        final var result = new StringBuilder();
        if (duration.toDaysPart() > 0) {
            result.append(duration.toDaysPart()).append(" days ");
        }
        if (duration.toHoursPart() > 0) {
            result.append(duration.toHoursPart()).append(" hours ");
        }
        if (duration.toMinutesPart() > 0) {
            result.append(duration.toMinutesPart()).append(" minutes ");
        }
        if (duration.toSecondsPart() > 0 || duration.toMillisPart() % 1000 > 0) {
            result.append(duration.toSecondsPart());
            if (duration.toMillisPart() % 1000 > 0) {
                result.append(duration.toMillisPart() % 1000 + 1000);
                result.setCharAt(result.length() - 4, '.');
            }
            result.append(" seconds");
        }
        return result.toString();
    }
}
