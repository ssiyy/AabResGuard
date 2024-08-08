package com.bytedance.android.aabresguard.siy;

import com.google.common.base.Strings;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * @author Siy
 * @since 2024/08/08
 */
public class SiyUtils {


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

    public static String convertStreamToString(InputStream is) {
        try {
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                return sb.toString();
            } else {
                return "";
            }
        } catch (IOException e) {
            return "";
        }
    }

    public static void deleteDirOrFile(final File file) {
        if (file == null) {
            return;
        }
        try {
            if (file.exists()) {
                FileUtils.forceDelete(file);
            }
        } catch (Exception e) {
            //
        }
    }
}
