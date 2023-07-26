package com.troblecodings.contentpacklib;

import java.io.File;
import java.io.FileFilter;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.resources.FilePack;
import net.minecraft.resources.FolderPack;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.IPackNameDecorator;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackInfo.IFactory;

public class CustomFolderPackFinder implements IPackFinder {

    private static final FileFilter RESOURCEPACK_FILTER = (p_195731_0_) -> {
        final boolean flag = p_195731_0_.isFile() && p_195731_0_.getName().endsWith(".zip");
        final boolean flag1 = p_195731_0_.isDirectory()
                && (new File(p_195731_0_, "pack.mcmeta")).isFile();
        return flag || flag1;
    };

    private final File folder;

    public CustomFolderPackFinder(final File file) {
        this.folder = file;
    }

    private Supplier<IResourcePack> createSupplier(final File file) {
        return file.isDirectory() ? () -> {
            return new FolderPack(file);
        } : () -> {
            return new FilePack(file);
        };
    }

    @Override
    public void loadPacks(Consumer<ResourcePackInfo> consumer, IFactory factory) {
        if (!this.folder.isDirectory()) {
            this.folder.mkdirs();
        }
        final File[] files = this.folder.listFiles(RESOURCEPACK_FILTER);
        if (files != null) {
            for (final File file : files) {
                final String s = "CP_" + file.getName();
                final ResourcePackInfo resourcepackinfo = ResourcePackInfo.create(s, true,
                        this.createSupplier(file), factory, ResourcePackInfo.Priority.TOP,
                        IPackNameDecorator.DEFAULT);
                if (resourcepackinfo != null) {
                    consumer.accept(resourcepackinfo);
                }
            }
        }
    }
}