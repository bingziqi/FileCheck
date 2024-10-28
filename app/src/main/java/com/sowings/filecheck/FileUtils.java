package com.sowings.filecheck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

public class FileUtils {
    public static List<UsbFile> getAllFiles(UsbFile root) throws IOException {
        List<UsbFile> allFiles = new ArrayList<>();
        if (root != null && root.isDirectory()) {
            UsbFile[] files = root.listFiles();
            if (null != files) {
                for (UsbFile file : files) {
                    if (file.isDirectory()) {
                        allFiles.addAll(getAllFiles(file));
                    } else {
                        allFiles.add(file);
                    }
                }
            }
        }
        return allFiles;
    }
}
