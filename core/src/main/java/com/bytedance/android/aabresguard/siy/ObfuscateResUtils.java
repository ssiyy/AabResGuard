package com.bytedance.android.aabresguard.siy;

import com.android.aapt.Resources;
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoElementBuilder;
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoNode;
import com.bytedance.android.aabresguard.siy.elf.ByteArrayProvider;
import com.bytedance.android.aabresguard.siy.elf.ElfHeader;
import com.bytedance.android.aabresguard.siy.elf.ElfSectionHeader;
import com.bytedance.android.aabresguard.siy.elf.RethrowContinuesFactory;

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
import java.util.Random;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * @author Siy
 * @since 2024/08/08
 */
public class ObfuscateResUtils {


    /**
     * 混淆资源文件
     *
     * @param bundleRawPath
     * @param orgByte
     * @return
     * @throws IOException
     */
    public static byte[] obfuscatorResContent(String bundleRawPath, String obfuscatedPath, byte[] orgByte) throws IOException {
        try {
            String extension = SiyUtils.getFileExtensionFromUrl(bundleRawPath).toLowerCase();
            if (isObfuscateImage(extension)) {
                return obfuscatorRandomPixel(bundleRawPath, orgByte, extension);
            } else if (isObfuscateXml(extension)) {
                return obfuscatorXml(bundleRawPath, orgByte);
            }
        } catch (Exception e) {
            //
        }
        return orgByte;
    }

    /**
     * 混淆不是资源文件会
     *
     * @param bundleRawPath
     * @param orgByte
     * @return
     */
    public static byte[] obfuscatorRawContent(String bundleRawPath, byte[] orgByte) {
        try {
            String extension = SiyUtils.getFileExtensionFromUrl(bundleRawPath).toLowerCase();
            if (isObfuscateImage(extension)) {
                return obfuscatorRandomPixel(bundleRawPath, orgByte, extension);
            } else if (isObfuscateXml(extension)) {
                return obfuscatorXml(bundleRawPath, orgByte);
            } else if (isObfuscateSo(extension)) {
                return obfuscateSo(bundleRawPath, orgByte);
            }
        } catch (Exception e) {
            //
        }
        return orgByte;
    }

    /**
     * 混淆图片随机像素点
     *
     * @param rawPath
     * @param orgByte
     * @param extension
     * @return
     */
    private static byte[] obfuscatorRandomPixel(String rawPath, byte[] orgByte, String extension) {
        try {
            String fileName = SiyUtils.getFileName(rawPath);
            //如果是点9图不混淆
            if (fileName.endsWith(".9.png")) {
                return orgByte;
            }

            InputStream inputStream = new ByteArrayInputStream(orgByte);
            BufferedImage imgsrc = ImageIO.read(inputStream);

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
            ConsoleColors.greenPrintln("obfuscate image result:" + rawPath + ":" + true);
            return afterByte;
        } catch (Exception e) {
            ConsoleColors.redPrintln("obfuscate image result:" + rawPath + ":" + false + ",msg:" + e.getMessage());
            return orgByte;
        }
    }

    /**
     * 将BufferedImage转换为byte[]
     *
     * @param image
     * @return
     */
    private static byte[] bufferedImageToByteArray(BufferedImage image, String extension) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, extension, os);
        byte[] b = os.toByteArray();
        os.close();
        return b;
    }

    /**
     * 混淆xml，插入随机字符串的命名空间
     *
     * @param orgByte
     * @return
     * @throws IOException
     */
    private static byte[] obfuscatorXml(String rawPath, byte[] orgByte) throws IOException {
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
            ConsoleColors.greenPrintln("obfuscate xml result:" + rawPath + ":true");
            return afterByte;
        } catch (Exception e) {
            ConsoleColors.redPrintln("obfuscate xml result:" + rawPath + ":false,msg:" + e.getMessage());
            return orgByte;
        }
    }

    public static boolean isObfuscateFile(String extension) {
        return isObfuscateImage(extension) || isObfuscateXml(extension) || isObfuscateSo(extension);
    }


    private static boolean isObfuscateImage(String extension) {
        return extension.endsWith("png") || extension.endsWith("jpg") || extension.endsWith("jpeg") || extension.endsWith("webp");
    }

    private static boolean isObfuscateXml(String extension) {
        return extension.endsWith("xml");
    }

    private static boolean isObfuscateSo(String extension) {
        return extension.endsWith("so");
    }


    /**
     * 混淆so库
     *
     * @param rawPath
     * @param bytes
     * @return
     * @throws IOException
     */
    private static byte[] obfuscateSo(String rawPath, byte[] bytes) throws IOException {
        //创建一个临时目录
        String relativePath = new File(rawPath).getParent();
        File orgSoFile = new File("org_so/" + relativePath);
        SiyUtils.deleteDirOrFile(orgSoFile);
        orgSoFile.mkdirs();

        //生成一个临时的so文件
        String soName = SiyUtils.getFileName(rawPath);
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


            executionCommand(cmdPath);

            //生成写入so库ELFHeader随机字符串
            Path outPutFile = Paths.get(destinationDir, "output.txt");
            String outPutFileContent = UUID.randomUUID() + ":" + String.valueOf(System.currentTimeMillis());
            Files.write(outPutFile, outPutFileContent.getBytes(), StandardOpenOption.CREATE);
            String outputFileString = outPutFile.toFile().getAbsolutePath();

            String keyStr = ".myobs";

            //插入随机字符串之后so生成的目录
            File obfuscatorSoFile = new File("obfuscator_so/" + relativePath);
            SiyUtils.deleteDirOrFile(obfuscatorSoFile);
            obfuscatorSoFile.mkdirs();
            String obfuscatorSoFileString = obfuscatorSoFile.getAbsolutePath() + "/" + soFile.getName();

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
            if (exitCode == 0) {
                printObfuscateSO(obfuscatorSoFileString);
                ConsoleColors.greenPrintln("obfuscate so result:" + rawPath + ":" + true);
                //如果混淆成功就替换原始的字节数组
                return Files.readAllBytes(Paths.get(obfuscatorSoFileString));
            } else {
                InputStream inputStream = process.getErrorStream();
                String str = SiyUtils.convertStreamToString(inputStream);
                ConsoleColors.redPrintln("obfuscate so result:" + rawPath + ":" + false + ",msg:" + str);
            }
        } catch (Exception e) {
            ConsoleColors.redPrintln("obfuscate so result:" + rawPath + ":" + false + ",msg:" + e.getMessage());
        }
        return bytes;
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

    private static void printObfuscateSO(String path) {
        try {
            ElfHeader elfHeader = ElfHeader.createElfHeader(RethrowContinuesFactory.INSTANCE, new ByteArrayProvider(Files.readAllBytes(Paths.get(path))));
            elfHeader.parse();

            ElfSectionHeader header = elfHeader.getSection(".myobs");
            ConsoleColors.normalPrintln("addSection is :" + header.getNameAsString() + ",data:" + new String(header.getData()) + ",flags:" + header.getFlags() + ",type:" + header.getTypeAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
