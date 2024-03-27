package com.bytedance.android.aabresguard.utils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Siy
 * @since 2024/03/27
 */
public class OS {
    private static final Logger LOGGER = Logger.getLogger("");

    public static void rmdir(File dir) throws Exception {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                File[] var2 = files;
                int var3 = files.length;

                for (int var4 = 0; var4 < var3; ++var4) {
                    File file = var2[var4];
                    if (file.isDirectory()) {
                        rmdir(file);
                    } else {
                        file.delete();
                    }
                }

                dir.delete();
            }
        }
    }

    public static void cpdir(File src, File dest) throws Exception {
        dest.mkdirs();
        File[] files = src.listFiles();
        if (files != null) {
            File[] var3 = files;
            int var4 = files.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                File file = var3[var5];
                File destFile = new File(dest.getPath() + File.separatorChar + file.getName());
                if (file.isDirectory()) {
                    cpdir(file, destFile);
                } else {
                    try {
                        FileInputStream in = new FileInputStream(file);

                        try {
                            FileOutputStream out = new FileOutputStream(destFile);

                            try {
                                IOUtils.copy(in, out);
                            } catch (Throwable var14) {
                                try {
                                    out.close();
                                } catch (Throwable var13) {
                                    var14.addSuppressed(var13);
                                }

                                throw var14;
                            }

                            out.close();
                        } catch (Throwable var15) {
                            try {
                                in.close();
                            } catch (Throwable var12) {
                                var15.addSuppressed(var12);
                            }

                            throw var15;
                        }

                        in.close();
                    } catch (IOException var16) {
                        throw new Exception("Could not copy file: " + file, var16);
                    }
                }
            }

        }
    }

    public static void exec(String[] cmd) throws Exception {
        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            Process ps = builder.start();
            (new OS.StreamForwarder(ps.getErrorStream(), "ERROR")).start();
            (new OS.StreamForwarder(ps.getInputStream(), "OUTPUT")).start();
            int exitValue = ps.waitFor();
            if (exitValue != 0) {
                throw new Exception("could not exec (exit code = " + exitValue + "): " + Arrays.toString(cmd));
            }
        } catch (IOException var4) {
            throw new Exception("could not exec: " + Arrays.toString(cmd), var4);
        } catch (InterruptedException var5) {
            throw new Exception("could not exec : " + Arrays.toString(cmd), var5);
        }
    }

    public static String execAndReturn(String[] cmd) {
        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            OS.StreamCollector collector = new OS.StreamCollector(process.getInputStream());
            executor.execute(collector);
            process.waitFor();
            if (!executor.awaitTermination(15L, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                    System.err.println("Stream collector did not terminate.");
                }
            }

            return collector.get();
        } catch (InterruptedException | IOException var5) {
            return null;
        }
    }

    static class StreamForwarder extends Thread {
        private final InputStream mIn;
        private final String mType;

        StreamForwarder(InputStream is, String type) {
            this.mIn = is;
            this.mType = type;
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.mIn));

                String line;
                while ((line = br.readLine()) != null) {
                    if (this.mType.equals("OUTPUT")) {
                        OS.LOGGER.info(line);
                    } else {
                        OS.LOGGER.warning(line);
                    }
                }
            } catch (IOException var3) {
                var3.printStackTrace();
            }

        }
    }

    static class StreamCollector implements Runnable {
        private final StringBuilder buffer = new StringBuilder();
        private final InputStream inputStream;

        public StreamCollector(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.inputStream));

                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        this.buffer.append(line).append('\n');
                    }
                } catch (Throwable var6) {
                    try {
                        reader.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }

                    throw var6;
                }

                reader.close();
            } catch (IOException var7) {
            }

        }

        public String get() {
            return this.buffer.toString();
        }
    }
}
