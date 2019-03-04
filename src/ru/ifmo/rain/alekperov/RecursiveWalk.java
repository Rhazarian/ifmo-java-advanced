package ru.ifmo.rain.alekperov;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    private static void showUsage() {
        System.out.printf("Usage: %s [path to the input file containing the list of the files and directories to be walked over] [path to the desired output file]%n", RecursiveWalk.class.getName());
    }

    private static void showFileError(final String action, final String path, final Exception ex) {
        System.out.printf("An error occurred while trying to %s %s:%nSystem message: %s%n", action, path, ex.getMessage());
    }

    private static void showReadError(final String path, final Exception ex) {
        showFileError("read from", path, ex);
    }

    private static void showWriteError(final String path, final Exception ex) {
        showFileError("write to", path, ex);
    }

    private static void showOpenError(final String path, final Exception ex) {
        showFileError("open", path, ex);
    }

    private static void showInvalidPathError(final Exception ex) {
        System.out.println("Invalid path:");
        System.out.println(ex.getMessage());
    }

    private static Path getPath(final String[] args, final int i) {
        try {
            return Paths.get(args[i]);
        } catch (InvalidPathException ex) {
            showInvalidPathError(ex);
            showUsage();
            return null;
        }
    }

    private static void writeHash(final BufferedWriter outputWriter, final int hash, final String file) throws IOException {
        try {
            outputWriter.write(String.format("%08x", hash));
            outputWriter.write(' ');
            outputWriter.write(file);
            outputWriter.newLine();
        } catch (IOException ex) {
            showWriteError(file, ex);
            throw ex;
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            showUsage();
            return;
        }
        final var input = getPath(args, 0);
        if (input == null) {
            return;
        }
        final var output = getPath(args, 1);
        if (output == null) {
            return;
        }

        try (final var inputReader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            try (final var outputWriter = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                try {
                    String line;
                    while ((line = inputReader.readLine()) != null) {
                        final Path path;
                        try {
                            path = Paths.get(line);
                        } catch (final InvalidPathException ex) {
                            showInvalidPathError(ex);
                            writeHash(outputWriter, 0, line);
                            continue;
                        }
                        try {
                            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                                private FileVisitResult processFail(final Path file) throws IOException {
                                    writeHash(outputWriter, 0, file.toString());
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                                    if (attrs.isDirectory()) {
                                        return FileVisitResult.CONTINUE;
                                    }
                                    final InputStream is;
                                    try {
                                        is = Files.newInputStream(file);
                                    } catch (IOException ex) {
                                        showOpenError(file.toString(), ex);
                                        return processFail(file);
                                    }
                                    final int hash;
                                    try {
                                        hash = FNV.get32BitHash(is);
                                    } catch (IOException ex) {
                                        showReadError(file.toString(), ex);
                                        return processFail(file);
                                    }
                                    writeHash(outputWriter, hash, file.toString());
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFileFailed(final Path file, final IOException ex) throws IOException {
                                    showOpenError(file.toString(), ex);
                                    return processFail(file);
                                }
                            });
                        } catch (final IOException ex) {
                            showWriteError(output.toString(), ex);
                            break;
                        }
                    }
                } catch (final IOException ex) {
                    showReadError(input.toString(), ex);
                }
            } catch (final IOException ex) {
                showOpenError(output.toString(), ex);
            }
        } catch (final IOException ex) {
            showOpenError(input.toString(), ex);
        }
    }

}