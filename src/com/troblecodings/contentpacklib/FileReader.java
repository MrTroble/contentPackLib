package com.troblecodings.contentpacklib;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.troblecodings.signals.OpenSignalsMain;

public class FileReader {

    private static FileSystem fileSystemCache = null;
    private final String modid;
    private final String internalBaseFolder;
    private final Logger logger;
    private final Function<String, Path> function;
    private final Gson gson;
    private final Path contentDirectory;
    private final List<Path> paths = new ArrayList<>();

    public FileReader(final String modid, final String internalBaseFolder, final Logger logger,
            final Function<String, Path> function) {
        this.modid = modid;
        this.internalBaseFolder = internalBaseFolder;
        this.logger = logger;
        this.function = function;
        this.gson = new Gson();
        this.contentDirectory =  Paths.get("contentpacks", modid);
        try {
            Files.createDirectories(contentDirectory);
            Files.list(contentDirectory).filter(path -> path.endsWith(".zip")).forEach(path -> {
                try {
                    paths.add(FileSystems.newFileSystem(path).getPath("."));
                } catch (IOException e) {
                    logger.error(String.format("Could not load %s!", path.toString()), e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String fromInternal(String internal) {
        return internalBaseFolder + "/" + internal;
    }

    public Map<String, String> getFiles(final String internal, final List<Path> paths) {
        final List<Path> internalPaths = new ArrayList<>();
        internalPaths.add(function.apply(fromInternal(internal)));
        internalPaths.addAll(paths);
        return getFiles(internalPaths);
    }

    public Map<String, String> getFiles(final List<Path> paths) {
        final Map<String, String> files = new HashMap<>();
        paths.forEach(path -> {
            try {
                final Stream<Path> inputs = Files.list(path);
                inputs.forEach(file -> {
                    try {
                        final String content = new String(Files.readAllBytes(file));
                        final String name = file.getFileName().toString();
                        files.put(name, content);
                    } catch (final IOException e) {
                        logger.warn("There was a problem during loading " + file + " !");
                        e.printStackTrace();
                    }
                });
                inputs.close();
            } catch (final IOException e) {
                logger.warn("There was a problem during listing all files from " + path + " !");
                e.printStackTrace();
            }
        });
        return files;
    }

    public Map<String, String> getFiles(final String internal) {
        final ArrayList<Path> externalPaths = new ArrayList<>();
        final String fullPath = fromInternal(internal);
        paths.forEach(path -> externalPaths.add(path.resolve(fullPath)));
        return getFiles(internal, externalPaths);
    }

    public <T> Map<String, T> toJson(final Class<T> clazz, final Map<String, String> file) {
        final Map<String, T> map = new HashMap<>();
        file.forEach((name, content) -> {
            map.put(name, gson.fromJson(content, clazz));
        });
        return map;
    }

}