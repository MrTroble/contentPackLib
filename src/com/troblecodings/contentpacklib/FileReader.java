package com.troblecodings.contentpacklib;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.io.file.SimplePathVisitor;
import org.apache.logging.log4j.Logger;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.troblecodings.signals.OpenSignalsMain;
import com.troblecodings.signals.init.OSBlocks;

public class FileReader {

	private static FileSystem fileSystemCache = null;
	private final String modid;
	private final String internalBaseFolder;
	private final Logger logger;
	private final Function<String, Path> function;
	private final Gson gson;

	public FileReader(final String modid, final String internalBaseFolder, final Logger logger,
			final Function<String, Path> function) {
		this.modid = modid;
		this.internalBaseFolder = internalBaseFolder;
		this.logger = logger;
		this.function = function;
		this.gson = new Gson();
	}

	public Map<String, String> getFiles(final String internal, final List<Path> paths) {
		final List<Path> internalPaths = new ArrayList<>();
		internalPaths.add(function.apply(internalBaseFolder + File.pathSeparator + internal));
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

	public <T> Map<String, T> toJson(final Class<T> clazz, final Map<String, String> file) {
		final Map<String, T> map = new HashMap<>();
		file.forEach((name, content) -> {
			map.put(name, gson.fromJson(content, clazz));
		});
		return map;
	}

	/**
	 * 
	 * @param directory : The path to the directory you want to read out from the
	 *                  resource folder of this mod
	 * @return a map containing all files, as key the filename and as value the
	 *         content of this file
	 */

	public static Map<String, String> readallFilesfromDierectory(final String directory) {
		final Map<String, String> files = new HashMap<>();
		final Optional<Path> filepath = getRessourceLocation(directory);
		if (filepath.isPresent()) {
			final Path pathlocation = filepath.get();
			try {
				final Stream<Path> inputs = Files.list(pathlocation);
				inputs.forEach(file -> {
					try {
						final String content = new String(Files.readAllBytes(file));
						final String name = file.getFileName().toString();

						files.put(name, content);
					} catch (final IOException e) {
						OpenSignalsMain.log.warn("There was a problem during loading " + file + " !");
						e.printStackTrace();
					}
				});
				inputs.close();
				return files;
			} catch (final IOException e) {
				OpenSignalsMain.log.warn("There was a problem during listing all files from " + pathlocation + " !");
				e.printStackTrace();
			}
		}
		if (files.isEmpty()) {
			OpenSignalsMain.getLogger().warn("No files found at " + directory + "!");
		}
		return files;
	}

	public static Optional<Path> getRessourceLocation(final String location) {
		return Optional.of(OpenSignalsMain.getInstance().file.findResource(location));
	}

	public static void addToFileSystem(final FileSystem system) {
		if (fileSystemCache == null)
			getRessourceLocation("");

		final URL url = OSBlocks.class.getResource("/assets/opensignals");
		try {
			final URI uri = url.toURI();
			final String scheme = uri.getScheme();
			Path path = null;
			if (scheme.equals("file")) {
				path = fileSystemCache.provider().getPath(uri);
			} else if (scheme.equals("jar")) {
				path = fileSystemCache.getPath("/");
			}
			if (path == null) {
				OpenSignalsMain.getLogger().error("[Error]: Could not get path to add to file system!");
				return;
			}
			final Path finalPath = path;
			Files.walkFileTree(system.getPath("/"), new SimplePathVisitor() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					Path nextPath = finalPath.resolve(file.getFileName().toString());
					ByteStreams.copy(Files.newInputStream(file), Files.newOutputStream(nextPath));
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final URISyntaxException | IOException e) {
			e.printStackTrace();
		}
	}
}