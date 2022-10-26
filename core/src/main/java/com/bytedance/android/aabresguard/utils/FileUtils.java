package com.bytedance.android.aabresguard.utils;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Created by YangJing on 2019/10/18 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class FileUtils {
    private static final Joiner UNIX_NEW_LINE_JOINER = Joiner.on('\n');

    /**
     * Loads a text file forcing the line separator to be of Unix style '\n' rather than being
     * Windows style '\r\n'.
     */
    public static String loadFileWithUnixLineSeparators(File file) throws IOException {
        checkFileExistsAndReadable(file.toPath());
        return UNIX_NEW_LINE_JOINER.join(Files.readLines(file, Charsets.UTF_8));
    }

    /**
     * Creates a new text file or replaces content of an existing file.
     *
     * @param file    the file to write to
     * @param content the new content of the file
     */
    public static void writeToFile(File file, String content) throws IOException {
        Files.createParentDirs(file);
        Files.asCharSink(file, StandardCharsets.UTF_8).write(content);
    }

    /**
     * 获取后缀
     * @param url
     * @return
     */
    public static String getFileExtensionFromUrl(String url) {
        if (!Strings.isNullOrEmpty(url)) {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) {
                url = url.substring(0, fragment);
            }

            int query = url.lastIndexOf('?');
            if (query > 0) {
                url = url.substring(0, query);
            }

            int filenamePos = url.lastIndexOf('/');
            String filename =
                    0 <= filenamePos ? url.substring(filenamePos + 1) : url;

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty() &&
                    Pattern.matches("[a-zA-Z_0-9\\.\\-\\(\\)\\%]+", filename)) {
                int dotPos = filename.lastIndexOf('.');
                if (0 <= dotPos) {
                    return filename.substring(dotPos + 1);
                }
            }
        }

        return "";
    }


    /**
     * Gets the file name in a pathname, AKA its last element.
     *
     * @param url A path.
     * @return The last element of the path, possibly the entire path for root paths.
     */
    public static String getFileName(String url) {
        if (!Strings.isNullOrEmpty(url)) {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) {
                url = url.substring(0, fragment);
            }

            int query = url.lastIndexOf('?');
            if (query > 0) {
                url = url.substring(0, query);
            }

            int filenamePos = url.lastIndexOf('/');
            String filename =
                    0 <= filenamePos ? url.substring(filenamePos + 1) : url;

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
          return filename;
        }

        return "";
    }
}
