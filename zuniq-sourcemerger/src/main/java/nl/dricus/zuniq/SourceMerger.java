package nl.dricus.zuniq;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

public final class SourceMerger {
    public static void main(String[] args) {
        merge(args[0], args[1]);
    }

    private static void merge(String inputDir, String outputFile) {
        File dir = new File(inputDir);
        File[] files = dir.listFiles((d, name) -> name.matches(".*\\.java"));
        Set<String> imports = new TreeSet<>();
        List<String> sourceLines = new ArrayList<>();

        if (files == null)
            throw new IllegalArgumentException(String.format("%s is not a directory!", inputDir));

        readFiles(files, imports, sourceLines);
        String merged = normalizeWhitespace(mergeLines(imports, sourceLines));
        writeToFile(outputFile, merged);
    }

    private static void readFiles(File[] files, Set<String> imports, List<String> sourceLines) {
        for (File file : files) {
            Stream<String> lines = readAllLines(file);

            lines.forEach(
                    line -> {
                        if (isImport(line))
                            imports.add(line);
                        else
                            sourceLines.add(line);
                    }
            );
        }
    }

    private static String mergeLines(Set<String> imports, List<String> sourceLines) {
        StringBuilder builder = new StringBuilder();

        for (String importLine : imports) {
            builder.append(importLine);
            builder.append('\n');
        }
        for (String sourceLine : sourceLines) {
            builder.append(sourceLine);
            builder.append('\n');
        }

        return builder.toString();
    }

    private static String normalizeWhitespace(String sourceLine) {
        return sourceLine.replaceAll("\\s+", " ");
    }

    private static Stream<String> readAllLines(File file) {
        try {
            return Files.lines(file.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isImport(String line) {
        return line.matches("import .*;");
    }

    private static void writeToFile(String outputFile, String merged) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(merged);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
