package com.bytedance.android.aabresguard.executors;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileDoesNotExist;
import static com.bytedance.android.aabresguard.bundle.AppBundleUtils.getEntryNameByResourceName;
import static com.bytedance.android.aabresguard.bundle.AppBundleUtils.getTypeNameByResourceName;
import static com.bytedance.android.aabresguard.bundle.ResourcesTableOperation.checkConfiguration;
import static com.bytedance.android.aabresguard.bundle.ResourcesTableOperation.updateEntryConfigValueList;
import static com.bytedance.android.aabresguard.utils.FileOperation.getFilePrefixByFileName;
import static com.bytedance.android.aabresguard.utils.FileOperation.getNameFromZipFilePath;
import static com.bytedance.android.aabresguard.utils.FileOperation.getParentFromZipFilePath;

import com.android.aapt.Resources;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.BundleModuleName;
import com.android.tools.build.bundletool.model.InMemoryModuleEntry;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.android.tools.build.bundletool.model.ResourceTableEntry;
import com.android.tools.build.bundletool.model.ZipPath;
import com.android.tools.build.bundletool.model.utils.ResourcesUtils;
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoElementBuilder;
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoNode;
import com.bytedance.android.aabresguard.bundle.AppBundleUtils;
import com.bytedance.android.aabresguard.bundle.ResourcesTableBuilder;
import com.bytedance.android.aabresguard.bundle.ResourcesTableOperation;
import com.bytedance.android.aabresguard.model.ResourcesMapping;
import com.bytedance.android.aabresguard.obfuscation.ResGuardStringBuilder;
import com.bytedance.android.aabresguard.parser.ResourcesMappingParser;
import com.bytedance.android.aabresguard.utils.ConsoleColors;
import com.bytedance.android.aabresguard.utils.FileOperation;
import com.bytedance.android.aabresguard.utils.FileUtils;
import com.bytedance.android.aabresguard.utils.OS;
import com.bytedance.android.aabresguard.utils.OSDetection;
import com.bytedance.android.aabresguard.utils.ResourceCopier;
import com.bytedance.android.aabresguard.utils.TimeClock;
import com.bytedance.android.aabresguard.utils.Utils;
import com.bytedance.android.aabresguard.utils.elf.ByteArrayProvider;
import com.bytedance.android.aabresguard.utils.elf.ElfHeader;
import com.bytedance.android.aabresguard.utils.elf.ElfSectionHeader;
import com.bytedance.android.aabresguard.utils.elf.RethrowContinuesFactory;
import com.bytedance.android.aabresguard.utils.ninepatch.GraphicsUtilities;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;


/**
 * Created by YangJing on 2019/10/14 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class ResourcesObfuscator {
    public static final String RESOURCE_ANDROID_PREFIX = "android:";
    public static final String FILE_MAPPING_NAME = "resources-mapping.txt";
    private static final Logger logger = Logger.getLogger(ResourcesObfuscator.class.getName());

    private final AppBundle rawAppBundle;
    private final Set<String> whiteListRules;
    private final Set<String> filterContentRules;
    private final Path outputMappingPath;
    private final ZipFile bundleZipFile;
    private ResourcesMapping resourcesMapping;

    public ResourcesObfuscator(Path bundlePath, AppBundle rawAppBundle, Set<String> whiteListRules, Set<String> filterContentRules, Path outputLogLocationDir, Path mappingPath) throws IOException {
        if (mappingPath != null && mappingPath.toFile().exists()) {
            resourcesMapping = new ResourcesMappingParser(mappingPath).parse();
        } else {
            resourcesMapping = new ResourcesMapping();
        }

        this.bundleZipFile = new ZipFile(bundlePath.toFile());

        outputMappingPath = new File(outputLogLocationDir.toFile(), FILE_MAPPING_NAME).toPath();
        checkFileDoesNotExist(outputMappingPath);

        this.rawAppBundle = rawAppBundle;
        this.whiteListRules = whiteListRules;
        this.filterContentRules = filterContentRules;
    }

    public Path getOutputMappingPath() {
        return outputMappingPath;
    }

    public AppBundle obfuscate() throws IOException {
        TimeClock timeClock = new TimeClock();

        checkResMappingRules();
        Map<BundleModuleName, BundleModule> obfuscatedModules = new HashMap<>();
        // generate type entry mapping from mapping rule
        Map<String, Set<String>> typeEntryMapping = generateObfuscatedEntryFilesFromMapping();

        for (Map.Entry<BundleModuleName, BundleModule> entry : rawAppBundle.getModules().entrySet()) {
            BundleModule bundleModule = entry.getValue();
            BundleModuleName bundleModuleName = entry.getKey();
            // generate obfuscation resources mapping
            generateResourceMappingRule(bundleModule, typeEntryMapping);
            // obfuscate module entries
            Map<String, String> obfuscateModuleEntriesMap = obfuscateModuleEntries(bundleModule, typeEntryMapping);
            // obfuscate bundle module
            BundleModule obfuscatedModule = obfuscateBundleModule(bundleModule, obfuscateModuleEntriesMap);
            obfuscatedModules.put(bundleModuleName, obfuscatedModule);
        }

        AppBundle appBundle = rawAppBundle.toBuilder()
                .setModules(ImmutableMap.copyOf(obfuscatedModules))
                .build();

        System.out.println(String.format(
                "obfuscate resources done, coast %s",
                timeClock.getCoast()
        ));

        // write mapping rules to file.
        resourcesMapping.writeMappingToFile(outputMappingPath);

        return appBundle;
    }

    private Map<String, Set<String>> generateObfuscatedEntryFilesFromMapping() {
        Map<String, Set<String>> typeEntryMapping = new HashMap<>();
        // generate obfuscated entry path from incremental mapping
        for (String path : resourcesMapping.getEntryFilesMapping().values()) {
            String parentPath = getParentFromZipFilePath(path);
            String name = getFilePrefixByFileName(getNameFromZipFilePath(path));
            Set<String> entryList = typeEntryMapping.get(parentPath);
            if (entryList == null) entryList = new HashSet<>();
            entryList.add(name);
            typeEntryMapping.put(parentPath, entryList);
        }
        // generate obfuscated entry name from incremental mapping
        for (String entry : resourcesMapping.getResourceMapping().values()) {
            String name = getEntryNameByResourceName(entry);
            String type = getTypeNameByResourceName(entry);
            Set<String> entryList = typeEntryMapping.get(type);
            if (entryList == null) entryList = new HashSet<>();
            entryList.add(name);
            typeEntryMapping.put(type, entryList);
        }
        return typeEntryMapping;
    }

    /**
     * Reads resourceTable and generate obfuscate mapping.
     */
    private void generateResourceMappingRule(BundleModule bundleModule, Map<String, Set<String>> typeEntryMapping) {
        if (!bundleModule.getResourceTable().isPresent()) {
            return;
        }
        ResGuardStringBuilder guardStringBuilder = new ResGuardStringBuilder();
        guardStringBuilder.reset(null);

        Resources.ResourceTable table = bundleModule.getResourceTable().get();
        // generate resource directory mapping
        ResourcesUtils.getAllFileReferences(table)
                .stream()
                .map(ZipPath::getParent)
                .filter(Objects::nonNull)
                .filter(path -> !resourcesMapping.getDirMapping().containsKey(path.toString()))
                .forEach(path -> {
                    guardStringBuilder.reset(null);
                    String name = guardStringBuilder.getReplaceString(resourcesMapping.getPathMappingNameList());
                    resourcesMapping.putDirMapping(path.toString(), BundleModule.RESOURCES_DIRECTORY.toString() + "/" + name);
                });
        // generate resource mapping
        ResourcesUtils.entries(table).forEach(entry -> {
            String resourceId = entry.getResourceId().toString();
            String resourceName = AppBundleUtils.getResourceFullName(entry);
            Set<String> obfuscationList = typeEntryMapping.get(entry.getType().getName());
            if (obfuscationList == null) {
                obfuscationList = new HashSet<>();
            }
            guardStringBuilder.reset(null);
            if (resourcesMapping.getResourceMapping().containsKey(resourceName)) {
                if (!shouldBeObfuscated(resourceName)) {
                    System.out.println(String.format(
                            "[whiteList] find whiteList resource, remove from mapping, resource: %s, id: %s",
                            resourceName,
                            resourceId
                    ));
                    resourcesMapping.getResourceMapping().remove(resourceName);
                } else {
                    String obfuscateResourceName = resourcesMapping.getResourceMapping().get(resourceName);
                    obfuscationList.add(AppBundleUtils.getEntryNameByResourceName(obfuscateResourceName));
                }
            } else {
                if (!shouldBeObfuscated(resourceName)) {
                    System.out.println(String.format(
                            "[whiteList] find whiteList resource, resource: %s, id: %s",
                            resourceName,
                            resourceId
                    ));
                } else {
                    String name = guardStringBuilder.getReplaceString(obfuscationList);
                    obfuscationList.add(name);
                    String obfuscatedResourceName = AppBundleUtils.getResourceFullName(entry.getPackage().getPackageName(), entry.getType().getName(), name);
                    resourcesMapping.putResourceMapping(resourceName, obfuscatedResourceName);
                }
            }
            typeEntryMapping.put(entry.getType().getName(), obfuscationList);
        });
    }

    /**
     * Obfuscate module entries and return the mapping rules.
     */
    private Map<String, String> obfuscateModuleEntries(BundleModule bundleModule, Map<String, Set<String>> typeMappingMap) {
        ResGuardStringBuilder guardStringBuilder = new ResGuardStringBuilder();
        guardStringBuilder.reset(null);
        Map<String, String> obfuscateEntries = new HashMap<>();

        bundleModule.getEntries().stream()
                .filter(entry -> entry.getPath().startsWith(BundleModule.RESOURCES_DIRECTORY))
                .forEach(entry -> {
                    guardStringBuilder.reset(null);
                    String entryDir = entry.getPath().getParent().toString();
                    String obfuscateDir = resourcesMapping.getDirMapping().get(entryDir);
                    if (obfuscateDir == null) {
                        throw new RuntimeException(String.format("can not find resource directory: %s", entryDir));
                    }
                    Set<String> mapping = typeMappingMap.get(obfuscateDir);
                    if (mapping == null) {
                        mapping = new HashSet<>();
                    }

                    String bundleRawPath = bundleModule.getName().getName() + "/" + entry.getPath().toString();
                    String bundleObfuscatedPath = resourcesMapping.getEntryFilesMapping().get(bundleRawPath);
                    if (bundleObfuscatedPath == null) {
                        if (!shouldBeObfuscated(bundleRawPath)) {
                            System.out.println(String.format(
                                    "[whiteList] find whiteList resource file, resource: %s",
                                    bundleRawPath
                            ));
                            return;
                        } else {
                            String fileSuffix = FileOperation.getFileSuffix(entry.getPath());
                            String obfuscatedName = guardStringBuilder.getReplaceString(mapping);
                            mapping.add(obfuscatedName);
                            bundleObfuscatedPath = obfuscateDir + "/" + obfuscatedName + fileSuffix;
                            resourcesMapping.putEntryFileMapping(bundleRawPath, bundleObfuscatedPath);
                        }
                    }
                    if (obfuscateEntries.values().contains(bundleObfuscatedPath)) {
                        throw new IllegalArgumentException(
                                String.format("Multiple entries with same key: %s -> %s",
                                        bundleRawPath, bundleObfuscatedPath)
                        );
                    }
                    obfuscateEntries.put(bundleRawPath, bundleObfuscatedPath);
                    typeMappingMap.put(obfuscateDir, mapping);
                });
        return obfuscateEntries;
    }

    /**
     * obfuscate bundle module.
     * 1. obfuscate bundle entries.
     * 2. obfuscate resourceTable.
     *
     * @param bundleModule       存储了所有文件信息
     * @param obfuscatedEntryMap 需要混淆的资源文件信息  key-资源路径，value-混淆之后的相对路径
     */
    private BundleModule obfuscateBundleModule(BundleModule bundleModule, Map<String, String> obfuscatedEntryMap) throws IOException {
        BundleModule.Builder builder = bundleModule.toBuilder();

        // obfuscate module entries
        List<ModuleEntry> obfuscateEntries = new ArrayList<>();
        for (ModuleEntry entry : bundleModule.getEntries()) {
            String bundleRawPath = bundleModule.getName().getName() + "/" + entry.getPath().toString();
            String obfuscatedPath = obfuscatedEntryMap.get(bundleRawPath);
            if (obfuscatedPath != null) {
                byte[] orgByte = AppBundleUtils.readByte(bundleZipFile, entry, bundleModule);
                byte[] obfuscatorByte = obfuscatorResContent(bundleRawPath, obfuscatedPath, orgByte);
                ModuleEntry obfuscatedEntry = InMemoryModuleEntry.ofFile(obfuscatedPath, obfuscatorByte);
                obfuscateEntries.add(obfuscatedEntry);
            } else {
                //如果不是资源文件会走到这儿来
                // root
                // assets
                // lib
                // dex
                String extension = FileUtils.getFileExtensionFromUrl(bundleRawPath).toLowerCase();
                if (isObfuscateFile(extension) && shouldBeFilterContent(bundleRawPath)) {
                    byte[] orgByte = AppBundleUtils.readByte(bundleZipFile, entry, bundleModule);

                    byte[] obfuscatorByte = obfuscatorRawContent(bundleRawPath, orgByte);
                    ModuleEntry obfuscatedEntry = InMemoryModuleEntry.ofFile(entry.getPath().toString(), obfuscatorByte);
                    obfuscateEntries.add(obfuscatedEntry);
                } else {
                    obfuscateEntries.add(entry);
                }
            }
        }
        builder.setRawEntries(obfuscateEntries);

        // obfuscate resourceTable
        Resources.ResourceTable obfuscatedResTable = obfuscateResourceTable(bundleModule, obfuscatedEntryMap);
        if (obfuscatedResTable != null) {
            builder.setResourceTable(obfuscatedResTable);
        }
        return builder.build();
    }

    private byte[] obfuscatorRawContent(String bundleRawPath, byte[] orgByte) {
        try {
            String extension = FileUtils.getFileExtensionFromUrl(bundleRawPath).toLowerCase();
            if (isObfuscateImage(extension)) {
                return obfuscatorRandomPixel(bundleRawPath, bundleRawPath, orgByte, extension);
            } else if (isObfuscateSo(extension)) {
                return obfuscateSo(bundleRawPath, orgByte);
            }
        } catch (Exception e) {
            //
        }
        return orgByte;
    }

    /**
     * 混淆资源文件
     *
     * @param bundleRawPath
     * @param orgByte
     * @return
     * @throws IOException
     */
    private byte[] obfuscatorResContent(String bundleRawPath, String obfuscatedPath, byte[] orgByte) throws IOException {
        try {
            if (!shouldBeFilterContent(bundleRawPath)) {
                return orgByte;
            }
            String extension = FileUtils.getFileExtensionFromUrl(bundleRawPath).toLowerCase();
            if (isObfuscateImage(extension)) {
                return obfuscatorRandomPixel(bundleRawPath, obfuscatedPath, orgByte, extension);
            } else if (isObfuscateXml(extension)) {
                return obfuscatorXml(bundleRawPath, obfuscatedPath, orgByte);
            }
        } catch (Exception e) {
            //
        }
        return orgByte;
    }

    /**
     * 混淆xml，插入随机字符串的命名空间
     *
     * @param rawPath
     * @param obfuscatedPath
     * @param orgByte
     * @return
     * @throws IOException
     */
    private byte[] obfuscatorXml(String rawPath, String obfuscatedPath, byte[] orgByte) throws IOException {
        try {
            Resources.XmlNode xmlNode = Resources.XmlNode.parseFrom(orgByte);
            XmlProtoNode xml = new XmlProtoNode(xmlNode);
            XmlProtoElementBuilder element = xml.toBuilder().getElement();

            String prefix = "magic_minify" + new Random().nextInt(9999);
            String RES_AUTO_NS = "http://schemas.android.com/apk/res-auto";
            byte[] afterByte = xml.toBuilder().setElement(element.addNamespaceDeclaration(prefix, RES_AUTO_NS))
                    .build()
                    .getProto()
                    .toByteArray();
            resourcesMapping.putXmlMapping(rawPath, obfuscatedPath, prefix + ":" + RES_AUTO_NS, DigestUtils.md5Hex(orgByte), DigestUtils.md5Hex(afterByte));
            return afterByte;
        } catch (Exception e) {
            resourcesMapping.putXmlMapping(rawPath, obfuscatedPath, e.getMessage(), DigestUtils.md5Hex(orgByte), DigestUtils.md5Hex(orgByte));
            return orgByte;
        }
    }


    /**
     * 混淆图片随机像素点
     *
     * @param rawPath
     * @param orgByte
     * @param extension
     * @return
     */
    private byte[] obfuscatorRandomPixel(String rawPath, String obfuscatedPath, byte[] orgByte, String extension) {
        try {
            String fileName = FileUtils.getFileName(rawPath);
            //如果是点9图不混淆
            if (fileName.endsWith(".9.png")) {
                return orgByte;
            }

            InputStream inputStream = new ByteArrayInputStream(orgByte);
            BufferedImage imgsrc = GraphicsUtilities.loadCompatibleImage(inputStream); // ImageIO.read(inputStream);

            int width = imgsrc.getWidth();
            int height = imgsrc.getHeight();
            //随机处理一个像素点
            if (width <= 5 || height <= 5) {
                return orgByte;
            }
            int w = Math.min(new Random().nextInt(width) + 2, width - 1);
            int h = Math.min(new Random().nextInt(height) + 2, height - 1);
            int pixel = imgsrc.getRGB(w, h);
            Color color = new Color(pixel);
            int red = color.getRed() + 1;
            if (red > 255) {
                red = 255;
            }

            int green = color.getGreen() - 1;
            if (green < 0) {
                green = 0;
            }

            int blue = color.getBlue() + 1;
            if (blue > 255) {
                blue = 255;
            }
            color = new Color(red, green, blue);
            imgsrc.setRGB(w, h, color.getRGB());

            byte[] afterByte = bufferedImageToByteArray(imgsrc, extension);
            resourcesMapping.putImageMapping(rawPath, obfuscatedPath, w, h, width, height, color, DigestUtils.md5Hex(orgByte), DigestUtils.md5Hex(afterByte));
            return afterByte;
        } catch (Exception e) {
            try {
                InputStream inputStream = new ByteArrayInputStream(orgByte);
                BufferedImage imgsrc = ImageIO.read(inputStream);
                resourcesMapping.putImageMapping(rawPath, obfuscatedPath, -1, -1, imgsrc.getWidth(), imgsrc.getHeight(), null, DigestUtils.md5Hex(orgByte), DigestUtils.md5Hex(orgByte));
            } catch (Exception ex) {
                resourcesMapping.putImageMapping(rawPath, obfuscatedPath, -1, -1, -1, -1, null, DigestUtils.md5Hex(orgByte), DigestUtils.md5Hex(orgByte));
            }
            return orgByte;
        }
    }

    /**
     * 将BufferedImage转换为byte[]
     *
     * @param image
     * @return
     */
    private byte[] bufferedImageToByteArray(BufferedImage image, String extension) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, extension, os);
        byte[] b = os.toByteArray();
        os.close();
        return b;
    }

    private boolean isObfuscateFile(String extension) {
        return isObfuscateImage(extension) || isObfuscateXml(extension) || isObfuscateSo(extension);
    }

    private boolean isObfuscateImage(String extension) {
        return extension.endsWith("png") || extension.endsWith("jpg") || extension.endsWith("jpeg") || extension.endsWith("webp");
    }

    private boolean isObfuscateXml(String extension) {
        return extension.endsWith("xml");
    }

    private boolean isObfuscateSo(String extension) {
        return extension.endsWith("so");
    }


    /**
     * Obfuscate resourceTable.
     */
    private Resources.ResourceTable obfuscateResourceTable(BundleModule bundleModule, Map<String, String> obfuscatedEntryMap) {
        if (!bundleModule.getResourceTable().isPresent()) {
            return null;
        }
        Resources.ResourceTable resourceTable = bundleModule.getResourceTable().get();

        ResourcesTableBuilder resourcesTableBuilder = new ResourcesTableBuilder();
        ResourcesUtils.entries(resourceTable).map(entry -> {
            String resourceName = AppBundleUtils.getResourceFullName(entry);
            String resourceId = entry.getResourceId().toString();
            String obfuscatedResName = resourcesMapping.getResourceMapping().get(resourceName);
            resourcesMapping.addResourceNameAndId(resourceName, resourceId);

            Resources.Entry obfuscatedEntry = entry.getEntry();
            if (obfuscatedResName != null) {
                // update entry name
                String entryName = getEntryNameByResourceName(obfuscatedResName);
                obfuscatedEntry = ResourcesTableOperation.updateEntryName(obfuscatedEntry, entryName);
            }

            // update config values
            List<Resources.ConfigValue> configValues = Stream.of(obfuscatedEntry)
                    .map(Resources.Entry::getConfigValueList)
                    .flatMap(Collection::stream)
                    .map(configValue -> {
                        if (!configValue.getValue().getItem().hasFile()) {
                            return configValue;
                        }
                        String rawPath = configValue.getValue().getItem().getFile().getPath();
                        String bundleRawPath = bundleModule.getName().getName() + "/" + rawPath;
                        String obfuscatedPath = obfuscatedEntryMap.get(bundleRawPath);
                        if (obfuscatedPath != null) {
                            resourcesMapping.addResourcePathAndId(bundleRawPath, resourceId);
                            resourcesMapping.putEntryFileMapping(bundleRawPath, obfuscatedPath);
                            return ResourcesTableOperation.replaceEntryPath(configValue, obfuscatedPath);
                        }
                        return configValue;
                    })
                    .collect(Collectors.toList());
            if (configValues.size() > 0) {
                obfuscatedEntry = updateEntryConfigValueList(obfuscatedEntry, configValues);
            }

            return ResourceTableEntry.create(entry.getPackage(), entry.getType(), obfuscatedEntry);
        }).forEach(entry -> {
            checkConfiguration(entry.getEntry());
            resourcesTableBuilder.addPackage(entry.getPackage()).addResource(entry.getType(), entry.getEntry());
        });

        return resourcesTableBuilder.build();
    }

    private void checkResMappingRules() {
        resourcesMapping.getDirMapping().values().stream()
                .map(ZipPath::create)
                .forEach(path -> {
                    if (!path.startsWith(BundleModule.RESOURCES_DIRECTORY)) {
                        throw new IllegalArgumentException(String.format(
                                "Module files can be only in pre-defined directories, the mapping obfuscation rule is %s",
                                path
                        ));
                    }
                });
    }

    private boolean shouldBeObfuscated(String resourceName) {
        // android system resources should not be obfuscated
        if (resourceName.startsWith(RESOURCE_ANDROID_PREFIX)) {
            return false;
        }
        for (String rule : whiteListRules) {
            Pattern filterPattern = Pattern.compile(Utils.convertToPatternString(rule));
            if (filterPattern.matcher(resourceName).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否应该被过滤
     *
     * @param rawPath
     * @return true 应该过滤不混淆 false不过滤，要混淆
     */
    private boolean shouldBeFilterContent(String rawPath) {
        for (String rule : filterContentRules) {
            if (rawPath.endsWith(rule)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 混淆so库
     *
     * @param rawPath
     * @param bytes
     * @return
     * @throws IOException
     */
    private byte[] obfuscateSo(String rawPath, byte[] bytes) throws IOException {
        //创建一个临时目录
        String relativePath = new File(rawPath).getParent();
        File orgSoFile = new File("org_so/" + relativePath);
        if (orgSoFile.exists()) {
            orgSoFile.delete();
        }
        orgSoFile.mkdirs();

        //生成一个临时的so文件
        String soName = FileUtils.getFileName(rawPath);
        File soFile = new File(orgSoFile, soName);
        FileOutputStream fos = new FileOutputStream(soFile);
        IOUtils.write(bytes, fos);

        try {
            //拷贝llvm命令，获取objcopy命令路径
            String sourceResourcesPath = "llvm/";
            String destinationDir = "llvm";
            ResourceCopier.copyResourcesFromJar(sourceResourcesPath, destinationDir);

            String cmdName = "";
            if (OSDetection.isWindows()) {
                cmdName = "windows" + File.separator + "llvm-objcopy.exe";
            } else if (OSDetection.isMacOSX()) {
                cmdName = "macosx" + File.separator + "llvm-objcopy";
            }

            Path objCopyPath = Paths.get(destinationDir, cmdName);
            String cmdPath = objCopyPath.toFile().getAbsolutePath();
            ConsoleColors.redPrintln("cmdPath:" + cmdPath);

            executionCommand(cmdPath);

            //生成写入so库ELFHeader随机字符串
            Path outPutFile = Paths.get(destinationDir, "output.txt");
            String outPutFileContent = UUID.randomUUID() + ":" + String.valueOf(System.currentTimeMillis());
            Files.write(outPutFile, outPutFileContent.getBytes(), StandardOpenOption.CREATE);
            String outputFileString = outPutFile.toFile().getAbsolutePath();
            ConsoleColors.redPrintln("outputFileString:" + outputFileString);

            String keyStr = ".mywaw";

            //插入随机字符串之后so生成的目录
            File obfuscatorSoFile = new File("obfuscator_so/" + relativePath);
            if (obfuscatorSoFile.exists()) {
                obfuscatorSoFile.delete();
            }
            obfuscatorSoFile.mkdirs();
            String obfuscatorSoFileString = obfuscatorSoFile.getAbsolutePath() + "/" + soFile.getName();
            ConsoleColors.redPrintln("obfuscatorSoFileString:" + obfuscatorSoFileString);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    cmdPath,
                    "--add-section",
                    keyStr + "=" + "" + outputFileString,
                    "--set-section-flags", keyStr + "=noload,readonly",
                    soFile.getAbsolutePath(),
                    obfuscatorSoFileString
            );

            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            ConsoleColors.greenPrintln("result:" + soFile.getAbsolutePath() + ":" + exitCode);
            if (exitCode == 0) {
                printObfuscateSO(obfuscatorSoFileString);
                //如果混淆成功就替换原始的字节数组
                return Files.readAllBytes(Paths.get(obfuscatorSoFileString));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    private static void printObfuscateSO(String path) {
        try {
            ElfHeader elfHeader = ElfHeader.createElfHeader(RethrowContinuesFactory.INSTANCE, new ByteArrayProvider(Files.readAllBytes(Paths.get(path))));
            elfHeader.parse();

            ElfSectionHeader header = elfHeader.getSection(".mywaw");
            ConsoleColors.normalPrintln("addSection is :" + header.getNameAsString() + ",data:" + new String(header.getData()) + ",flags:" + header.getFlags() + ",type:" + header.getTypeAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void executionCommand(String filePath) {
        try {
            File file = new File(filePath);
            if (file.canRead() && file.exists()) {
                boolean result = file.setExecutable(true);
                if (!result) {
                    ConsoleColors.redPrintln("cmd can not executable");
                    if (OSDetection.isMacOSX()) {
                        OS.exec(new String[]{"chomd", "755", filePath});
                        ConsoleColors.redPrintln("cmd can not executable 755 suc");
                    }
                }
            } else {
                ConsoleColors.redPrintln("cmd can not read");
            }
        } catch (Exception e) {
            ConsoleColors.redPrintln("cmd 755 " + e.getMessage());
        }
    }
}
