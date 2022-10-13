/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Lachlan Dowding
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.bytedance.android.aabresguard.utils.xml;


import com.android.aapt.Resources;
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoElementBuilder;
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoNode;

import java.io.IOException;
import java.util.Random;


/**
 * A collection of convenience methods for minifying XML.
 */
public final class XMLMinificationHelper {
    /**
     * Disallow instantiation of this class.
     */
    private XMLMinificationHelper() {
    }


    /**
     * Removes extraneous whitespace and comments from the given XML content.
     *
     * @param content The XML content to be minified.
     * @return The minified XML content.
     * @throws IOException When an IO error occurs.
     */
    public static byte[] minify(byte[] content) throws IOException {
        if (content == null) return null;

        try {
            //翻译成明文
            Resources.XmlNode xmlNode = Resources.XmlNode.parseFrom(content);
            XmlProtoNode xml = new XmlProtoNode(xmlNode);
            XmlProtoElementBuilder element = xml.toBuilder().getElement();
            return xml.toBuilder().setElement(element.addNamespaceDeclaration("magic_minify" + new Random().nextInt(9999), "http://schemas.android.com/apk/res-auto")).build().getProto().toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }
}
