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
        boolean flag = p_195731_0_.isFile() && p_195731_0_.getName().endsWith(".zip");
        boolean flag1 = p_195731_0_.isDirectory()
                && (new File(p_195731_0_, "pack.mcmeta")).isFile();
        return flag || flag1;
    };

    private final File folder;
    private final IPackNameDecorator packSource;

    public CustomFolderPackFinder(final File file, final IPackNameDecorator packSource) {
        this.folder = file;
        this.packSource = packSource;
    }

    @Override
    public void loadPacks(Consumer<ResourcePackInfo> p_230230_1_, IFactory p_230230_2_) {
        if (!this.folder.isDirectory()) {
            this.folder.mkdirs();
        }

        File[] afile = this.folder.listFiles(RESOURCEPACK_FILTER);
        if (afile != null) {
            for (File file1 : afile) {
                String s = "file/" + file1.getName();
                ResourcePackInfo resourcepackinfo = ResourcePackInfo.create(s, true,
                        this.createSupplier(file1), p_230230_2_, ResourcePackInfo.Priority.TOP,
                        this.packSource);
                if (resourcepackinfo != null) {
                    p_230230_1_.accept(resourcepackinfo);
                }
            }
        }
    }

    private Supplier<IResourcePack> createSupplier(File p_195733_1_) {
        return p_195733_1_.isDirectory() ? () -> {
            return new FolderPack(p_195733_1_);
        } : () -> {
            return new FilePack(p_195733_1_);
        };
    }
}