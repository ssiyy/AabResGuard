package com.bytedance.android.aabresguard.parser;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

import com.bytedance.android.aabresguard.model.xml.AabResGuardConfig;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.nio.file.Path;
import java.util.Iterator;

/**
 * Created by YangJing on 2019/10/14 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class AabResGuardXmlParser {
    private final Path configPath;

    public AabResGuardXmlParser(Path configPath) {
        checkFileExistsAndReadable(configPath);
        this.configPath = configPath;
    }

    public AabResGuardConfig parse() throws DocumentException {
        AabResGuardConfig aabResGuardConfig = new AabResGuardConfig();
        SAXReader reader = new SAXReader();
        Document doc = reader.read(configPath.toFile());
        Element root = doc.getRootElement();
        for (Iterator i = root.elementIterator("issue"); i.hasNext(); ) {
            Element element = (Element) i.next();

            String isActive = element.attributeValue("isactive");
            boolean active = isActive != null && isActive.equals("true");
            String id = element.attributeValue("id");
            switch (id){
                case "whitelist":
                    aabResGuardConfig.setUseWhiteList(active);
                    readWhiteListFromXml(aabResGuardConfig,element);
                    break;
                case "filterContent":
                    aabResGuardConfig.setUseFilterContent(active);
                    readFilterContent(aabResGuardConfig,element);
                    break;
            }
        }

        // file filter
        aabResGuardConfig.setFileFilter(new FileFilterXmlParser(configPath).parse());

        // string filter
        aabResGuardConfig.setStringFilterConfig(new StringFilterXmlParser(configPath).parse());

        return aabResGuardConfig;
    }

    private void readWhiteListFromXml(AabResGuardConfig aabResGuardConfig,Element element) {
        for (Iterator rules = element.elementIterator("path"); rules.hasNext(); ) {
            Element ruleElement = (Element) rules.next();
            String rule = ruleElement.attributeValue("value");
            if (rule != null) {
                aabResGuardConfig.addWhiteList(rule);
            }
        }
    }

    private void readFilterContent(AabResGuardConfig aabResGuardConfig,Element element) {
        for (Iterator rules = element.elementIterator("path"); rules.hasNext(); ) {
            Element ruleElement = (Element) rules.next();
            String rule = ruleElement.attributeValue("value");
            if (rule != null) {
                aabResGuardConfig.addFilterContent(rule);
            }
        }
    }
}
