package com.troblecodings.contentpacklib;

import java.io.File;
import java.io.FileFilter;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.resources.FilePack;
import net.minecraft.resources.FolderPack;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackInfo.IFactory;

public class CustomFolderPackFinder implements IPackFinder {

    private static final FileFilter RESOURCEPACK_FILTER = (p_195731_0_) -> {
        boolean flag = p_195731_0_.isFile() && p_195731_0_.getName().endsWith(".zip");
        boolean flag1 = p_195731_0_.isDirectory()
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ResourcePackInfo> void loadPacks(Map<String, T> map, IFactory<T> factory) {
        if (!this.folder.isDirectory()) {
            this.folder.mkdirs();
        }

        final File[] afile = this.folder.listFiles(RESOURCEPACK_FILTER);
        if (afile != null) {
            for (final File file1 : afile) {
                final String s = "CP_" + file1.getName();
                final ResourcePackInfo resourcepackinfo = ResourcePackInfo.create(s, true,
                        this.createSupplier(file1), factory, ResourcePackInfo.Priority.TOP);
                if (resourcepackinfo != null) {
                    map.put(s, (T) resourcepackinfo);
                }
            }
        }
    }
}