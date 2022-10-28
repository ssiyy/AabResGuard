package com.bytedance.android.aabresguard.model;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by YangJing on 2019/10/14 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class ResourcesMapping {

    private Map<String, String> dirMapping = new HashMap<>();
    private Map<String, String> resourceMapping = new HashMap<>();
    private Map<String, String> entryFilesMapping = new HashMap<>();

    private Map<String, String> resourcesNameToIdMapping = new HashMap<>();
    private Map<String, String> resourcesPathToIdMapping = new HashMap<>();

    /**
     * 图片随机加像素点
     */
    private Map<String, String> imageMapping = new HashMap<>();

    /**
     * xml文件随机加命名空间
     */
    private Map<String, String> xmlMapping = new HashMap<>();

    public ResourcesMapping() {
    }

    public static String getResourceSimpleName(String resourceName) {
        String[] values = resourceName.split("/");
        return values[values.length - 1];
    }

    public Map<String, String> getDirMapping() {
        return dirMapping;
    }

    public Map<String, String> getResourceMapping() {
        return resourceMapping;
    }

    public Map<String, String> getEntryFilesMapping() {
        return entryFilesMapping;
    }

    public void putDirMapping(String rawPath, String obfuscatePath) {
        dirMapping.put(rawPath, obfuscatePath);
    }

    public void putResourceMapping(String rawResource, String obfuscateResource) {
        if (resourceMapping.values().contains(obfuscateResource)) {
            throw new IllegalArgumentException(
                    String.format("Multiple entries: %s -> %s",
                            rawResource, obfuscateResource)
            );
        }
        resourceMapping.put(rawResource, obfuscateResource);
    }

    public void putEntryFileMapping(String rawPath, String obfuscatedPath) {
        entryFilesMapping.put(rawPath, obfuscatedPath);
    }

    public void putImageMapping(String rawPath, int x, int y, int width, int height, Color color, String orgMd5, String afterMd5) {
        int red = -1;
        int green = -1;
        int blue = -1;
        if (color != null) {
            red = color.getRed();
            green = color.getGreen();
            blue = color.getBlue();
        }
        imageMapping.put(rawPath, "(" + x + "," + y + ") of (w:" + width + ",h:" + height + ")" +
                "->rgb:(" + red + "," + green + "," + blue + ")," +
                "md5:" + orgMd5 + " -> " + afterMd5 + "," +
                "result:" + !Objects.equals(orgMd5, afterMd5));
    }

    public void putXmlMapping(String rawPath, String namespace, String orgMd5, String afterMd5) {
        xmlMapping.put(rawPath, namespace + ",md5:" + orgMd5 + " -> " + afterMd5 + (",result:" + !Objects.equals(orgMd5, afterMd5)));
    }

    public List<String> getPathMappingNameList() {
        return dirMapping.values().stream()
                .map(value -> {
                    String[] values = value.split("/");
                    if (value.length() == 0) return value;
                    return values[values.length - 1];
                })
                .collect(Collectors.toList());
    }

    public void addResourceNameAndId(String name, String id) {
        resourcesNameToIdMapping.put(name, id);
    }

    public void addResourcePathAndId(String path, String id) {
        resourcesPathToIdMapping.put(path, id);
    }

    /**
     * Write mapping rules to file.
     */
    public void writeMappingToFile(Path mappingPath) throws IOException {
        Writer writer = new BufferedWriter(new FileWriter(mappingPath.toFile(), false));

        // write resources dir
        writer.write("res dir mapping:\n");
        for (Map.Entry<String, String> entry : dirMapping.entrySet()) {
            writer.write(String.format(
                    "\t%s -> %s\n",
                    entry.getKey(),
                    entry.getValue()
            ));
        }
        writer.write("\n\n");
        writer.flush();

        // write resources name
        writer.write("res id mapping:\n");
        for (Map.Entry<String, String> entry : resourceMapping.entrySet()) {
            writer.write(String.format(
                    "\t%s : %s -> %s\n",
                    resourcesNameToIdMapping.get(entry.getKey()),
                    entry.getKey(),
                    entry.getValue()
            ));
        }
        writer.write("\n\n");
        writer.flush();

        // write resources entries path
        writer.write("res entries path mapping:\n");
        for (Map.Entry<String, String> entry : entryFilesMapping.entrySet()) {
            writer.write(String.format(
                    "\t%s : %s -> %s\n",
                    resourcesPathToIdMapping.get(entry.getKey()),
                    entry.getKey(),
                    entry.getValue()
            ));
        }
        writer.write("\n\n");
        writer.flush();

        // write image mapping
        writer.write("res image mapping :\n");
        for (Map.Entry<String, String> entry : imageMapping.entrySet()) {
            writer.write(String.format(
                    "\t%s  -> %s\n",
                    entry.getKey(),
                    entry.getValue()
            ));
        }
        writer.write("\n\n");
        writer.flush();

        // write xml mapping
        writer.write("res xmlMapping mapping :\n");
        for (Map.Entry<String, String> entry : xmlMapping.entrySet()) {
            writer.write(String.format(
                    "\t%s  -> %s\n",
                    entry.getKey(),
                    entry.getValue()
            ));
        }
        writer.write("\n\n");
        writer.flush();

        writer.close();
    }
}
