/*
 * Copyright 2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Session;

/**
 * Object that searches for HTML links in a content string and replaces
 * the link with valid URL links.
 */
public class DocumentPathReplacer  {

    private static final Pattern PATTERN = Pattern.compile("((?:<a\\s.*?href=\"))([^:]*?)(\".*?>)");
    
    private URLPathTranslator urlPathTranslator;

    /**
     * Constructor
     */
    DocumentPathReplacer(final Context context) {
        this.urlPathTranslator = new URLPathTranslator(context.getContextPath(), context);
    }
    
    /**
     * Search some content for hrefs and replace the document path link with a url link.   
     */
    public String replace(final Session jcrSession, final String content) {
        
         // only create if really needed
        StringBuffer sb = null; 
        
        Matcher m = PATTERN.matcher(content);
        m.reset();
        while (m.find()) {
            
            if (sb == null) { 
                sb = new StringBuffer(content.length());
            }
                        
            String documentPath = m.group(2);
            String url = urlPathTranslator.documentPathToURL(jcrSession, documentPath);
            m.appendReplacement(sb, m.group(1) + url + m.group(3));
        }
        
        if (sb == null) {
            return content;
        } else {
            m.appendTail(sb);
            return sb.toString();
        }
    }

}
