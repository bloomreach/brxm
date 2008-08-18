/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.template.node.content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;

/**
 * Object that searches for HTML links in a content string and replaces
 * the link with valid URL links.
 */
public class SourceRewriterImpl implements SourceRewriter {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Pattern HREF_PATTERN = Pattern.compile("((?:<a\\s.*?href=\"))([^:]*?)(\".*?>)");
    private static final Pattern SRC_PATTERN = Pattern.compile("((?:<img\\s.*?src=\"))([^:]*?)(\".*?>)");

    private PathTranslator urlPathTranslator;

    /*
     * constructor with default URLPathTranslatorImpl();
     */
    public SourceRewriterImpl() {
        this.urlPathTranslator = new PathTranslatorImpl();
    }
    
    /*
     * constructor with custom URLPathTranslatorImpl();
     */
    public SourceRewriterImpl(PathTranslator urlPathTranslator) {
        this.urlPathTranslator = urlPathTranslator ;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.template.node.content.SourceRewriter#replace(javax.jcr.Session, java.lang.String)
     */
    public String replace(final Node node,String content) {

        // only create if really needed
        StringBuffer sb = null;

        Matcher hrefPatt = HREF_PATTERN.matcher(content);
        hrefPatt.reset();
        while (hrefPatt.find()) {
            if (sb == null) {
                sb = new StringBuffer(content.length());
            }
            String documentPath = hrefPatt.group(2);
            String url = urlPathTranslator.documentPathToURL(node, documentPath);
            hrefPatt.appendReplacement(sb, hrefPatt.group(1) + url + hrefPatt.group(3));
        }
        
        if (sb != null) {
            hrefPatt.appendTail(sb);
            content = String.valueOf(sb);
            sb = null;
        }
        
        Matcher srcPatt = SRC_PATTERN.matcher(content);
        srcPatt.reset();
        while (srcPatt.find()) {
            if (sb == null) {
                sb = new StringBuffer(content.length());
            }
            String documentPath = srcPatt.group(2);
            String url = urlPathTranslator.documentPathToURL(node, documentPath);
            srcPatt.appendReplacement(sb, srcPatt.group(1) + url + srcPatt.group(3));
        }

        if (sb == null) {
            return content;
        } else {
            srcPatt.appendTail(sb);
            return sb.toString();
        }
    }

}
