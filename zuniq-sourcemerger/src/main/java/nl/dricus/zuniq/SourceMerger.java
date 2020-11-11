package nl.dricus.zuniq;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SourceMerger {
    public static final String MAIN_CLASS = "Zuniq";

    public static void main(String[] args) {
        merge(args[0], args[1]);
    }

    private static void merge(String inputDir, String outputFile) {
        List<File> all = new ArrayList<>();
        Set<String> imports = new TreeSet<>();
        List<String> sourceLines = new ArrayList<>();
        List<File[]> filelist = new ArrayList<>();
        File[] allfiles = null;
        addTree(new File(inputDir), all);
        String notToBeImported = getDirName(all);

        for (File dir : all) {
            //you just want to get the dir, not yet the files
            if (dir.getName().matches((".*\\.java"))) {
                continue;
            }
            //collecting the files in the dir
            allfiles = dir.listFiles((d, name) -> name.matches(".*\\.java"));
            filelist.add(allfiles);
        }

        for (File[] allFiles : filelist) {
            if (allFiles == null)
                throw new IllegalArgumentException(String.format("%s is not a directory!", inputDir));
            readFiles(allFiles, imports, sourceLines, notToBeImported);
        }
        String merged = normalizeWhitespace(mergeLines(imports, sourceLines));
        writeToFile(outputFile, merged);
    }

    private static String getDirName(List<File> all) {
        String directory = all.get(0).toString();
        System.out.println("dir " + directory);
        Pattern p = Pattern.compile("(?<=src\\/main\\/java\\/)(\\w+)");
        Matcher m = p.matcher(directory);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    static void addTree(File file, Collection<File> all) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                all.add(child);
                addTree(child, all);
            }
        }
    }

    private static void readFiles(File[] files, Set<String> imports, List<String> sourceLines, String notToBeImported) {
        for (File file : files) {
            Stream<String> lines = readAllLines(file);
            lines.forEach(
                line -> {
                    line = line.replaceFirst("public class", "class");
                    line = line.replaceFirst("public enum", "enum");
                    line = line.replaceFirst("public interface", "interface");
                    line = line.replaceFirst("class " + MAIN_CLASS, "public class " + MAIN_CLASS);
                    //delete comments
                    line = line.replaceAll(".*\\/\\/.*", "");
                    if (isImport(line)) {
                        line = line.replaceAll(".*" + notToBeImported + ".*", "");
                        imports.add(line);
                    }
                    if (isPackage(line)) {
                        //do nothing
                    } else {
                        sourceLines.add(line);
                        if (isImport(line)) {
                            sourceLines.remove(line);
                        }
                    }
                }
            );
        }
    }

    private static String mergeLines(Set<String> imports, List<String> sourceLines) {
        StringBuilder builder = new StringBuilder();

        for (String importLine : imports) {
            builder.append(importLine);
            builder.append("\n");
        }
        for (String sourceLine : sourceLines) {
            builder.append(sourceLine);
            builder.append("\n");
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
        return line.matches(".*import.*;");
    }

    private static boolean isPackage(String line) {
        return line.matches(".*package.*");
    }

    private static void writeToFile(String outputFile, String merged) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(merged);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
