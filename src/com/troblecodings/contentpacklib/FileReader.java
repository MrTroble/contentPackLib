package com.troblecodings.contentpacklib;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.gson.Gson;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FileReader {

	@SuppressWarnings("unused")
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
		this.contentDirectory = Paths.get("./contentpacks", modid);
		
		if(FMLCommonHandler.instance().getSide().isClient()) {
			((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
					.registerReloadListener(r -> this.onResourceManagerReload(r));
		}

		try {
			Files.createDirectories(contentDirectory);
			Files.list(contentDirectory).filter(path -> path.toString().endsWith(".zip")).forEach(path -> {
				try {
					paths.add(FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader()).getRootDirectories()
							.iterator().next());

				} catch (final IOException e) {
					logger.error(String.format("Could not load %s!", path.toString()), e);
				}
			});
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public List<Path> getPaths() {
		return this.paths;
	}

	private String fromInternal(final String internal) {
		return internalBaseFolder + "/" + internal;
	}

	public List<Entry<String, String>> getFiles(final String internal, final List<Path> paths) {
		final List<Path> internalPaths = new ArrayList<>();
		internalPaths.add(function.apply(fromInternal(internal)));
		internalPaths.addAll(paths);
		return getFiles(internalPaths);
	}

	public List<Entry<String, String>> getFiles(final List<Path> paths) {
		final List<Entry<String, String>> files = new ArrayList<>();
		paths.forEach(path -> {
			try {
				if (!(Files.exists(path) && Files.isDirectory(path)))
					return;
				final Stream<Path> inputs = Files.list(path);
				inputs.forEach(file -> {
					try {
						final String content = new String(Files.readAllBytes(file));
						final String name = file.getFileName().toString();
						files.add(Maps.immutableEntry(name, content));
					} catch (final IOException e) {
						logger.warn("There was a problem during reading " + file + " !");
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

	public List<Entry<String, String>> getFiles(final String internal) {
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

	@SideOnly(Side.CLIENT)
	public void onResourceManagerReload(IResourceManager resourceManager) {
		if (resourceManager instanceof SimpleReloadableResourceManager) {
			final SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager) resourceManager;

			try {
				Files.list(contentDirectory)
						.forEach(path -> manager.reloadResourcePack(new FileResourcePack(path.toFile())));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}