package io.github.gaming32.awremap;

import net.fabricmc.mappingio.tree.MappingTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class AwRemapper {
    private static final Pattern V1_SEPARATOR = Pattern.compile("\\s+");
    private static final Pattern V2_SEPARATOR = Pattern.compile("[ \\t]+");

    private final MappingTree mappings;
    private final BufferedReader reader;
    private final Writer writer;

    public AwRemapper(MappingTree mappings, Reader reader, Writer writer) {
        this.mappings = mappings;
        this.reader = reader instanceof BufferedReader bufferedReader ? bufferedReader : new BufferedReader(reader);
        this.writer = writer;
    }

    public long remap() throws IOException {
        final var entries = new AtomicLong();
        final var separator = readHeader();
        final Consumer<String[]> lineHandler = line -> {
            if (line.length == 0) {
                return;
            }

            switch (line[1]) {
                case "class" -> handleClass(line);
                case "field" -> handleMember(line, MappingTree::getField);
                case "method" -> handleMember(line, MappingTree::getMethod);
            }

            if (entries.getPlain() < Long.MAX_VALUE) {
                entries.setPlain(entries.getPlain() + 1);
            }
        };
        while (handleLine(separator, lineHandler)) {
            // Do nothing
        }
        return entries.getPlain();
    }

    private Pattern readHeader() throws IOException {
        return handleLine(V1_SEPARATOR, parts -> switch (parts[1]) {
            case "v1" -> V1_SEPARATOR;
            case "v2" -> V2_SEPARATOR;
            default -> throw new IllegalStateException("Invalid header " + String.join(" ", parts));
        }).orElseThrow(() -> new IllegalStateException("Missing header"));
    }

    private void handleClass(String[] line) {
        final var classMapping = mappings.getClass(line[2]);
        if (classMapping != null) {
            line[2] = classMapping.getName(0);
        }
    }

    private void handleMember(String[] line, MemberGetter getter) {
        final var mapping = getter.get(mappings, line[2], line[3], line[4]);
        if (mapping != null) {
            line[2] = mapping.getOwner().getName(0);
            line[3] = mapping.getName(0);
            line[4] = mapping.getDesc(0);
        } else {
            // Probably a constructor
            final var classMapping = mappings.getClass(line[2]);
            if (classMapping != null) {
                line[2] = classMapping.getName(0);
                line[4] = mappings.mapDesc(line[4], 0);
            }
        }
    }

    private boolean handleLine(Pattern separator, Consumer<String[]> handler) throws IOException {
        return handleLine(separator, parts -> {
            handler.accept(parts);
            return true;
        }).orElse(false);
    }

    private <T> Optional<T> handleLine(Pattern separator, Function<String[], T> handler) throws IOException {
        final var line = reader.readLine();
        if (line == null) {
            return Optional.empty();
        }

        final var commentIndex = line.indexOf('#');
        final var lineBody = commentIndex == -1 ? line : line.substring(0, commentIndex);
        final var comment = commentIndex == -1 ? "" : line.substring(commentIndex);

        final var bodyParts = new ArrayList<String>();
        final var separators = new ArrayList<String>();

        final var matcher = separator.matcher(lineBody);
        var start = 0;
        while (matcher.find()) {
            bodyParts.add(lineBody.substring(start, matcher.start()));
            separators.add(matcher.group());
            start = matcher.end();
        }
        if (start < lineBody.length()) {
            bodyParts.add(lineBody.substring(start));
            separators.add("");
        }

        final var bodyPartsArray = bodyParts.toArray(new String[0]);
        final var result = handler.apply(bodyPartsArray);

        for (int i = 0; i < separators.size(); i++) {
            writer.write(bodyPartsArray[i]);
            writer.write(separators.get(i));
        }
        if (!comment.isEmpty()) {
            writer.write(comment);
        }
        writer.write('\n');

        return Optional.of(result);
    }

    @FunctionalInterface
    private interface MemberGetter {
        MappingTree.MemberMapping get(MappingTree tree, String owner, String name, String desc);
    }
}
