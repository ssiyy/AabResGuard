package com.bytedance.android.aabresguard.executors;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.jar.*;

public class ResourceCopier {

    public static void copyResourcesFromJar(String sourceResourcesPath, String destinationDir) throws IOException {
        ClassLoader classLoader = ResourceCopier.class.getClassLoader();
        URL resourceUrl = classLoader.getResource(sourceResourcesPath);

        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource directory not found: " + sourceResourcesPath);
        }

        if ("jar".equals(resourceUrl.getProtocol())) {
            try (JarFile jarFile = ((JarURLConnection) resourceUrl.openConnection()).getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // 只处理源资源目录下的文件
                    if (entryName.startsWith(sourceResourcesPath) && !entryName.endsWith("/") && !entry.isDirectory()) {
                        // 去除源资源目录路径，得到相对路径
                        String relativePath = entryName.substring(sourceResourcesPath.length());
                        Path destinationPath = Paths.get(destinationDir, relativePath);

                        // 确保目标目录存在
                        Files.createDirectories(destinationPath.getParent());

                        try (InputStream input = jarFile.getInputStream(entry);
                             OutputStream output = Files.newOutputStream(destinationPath)) {

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = input.read(buffer)) > 0) {
                                output.write(buffer, 0, length);
                            }
                        }
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported protocol: " + resourceUrl.getProtocol());
        }
    }
}