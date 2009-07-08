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
package org.hippoecm.hst.utils;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

/**
 * Simple HTML Tag Extractor
 * 
 * @version $Id$
 */
public class SimpleHtmlExtractor {
    
    private static final int BUF_SIZE = 512;
    
    private static boolean htmlCleanerInitialized;
    private static HtmlCleaner cleaner;
    
    private SimpleHtmlExtractor() {
    }
    
    static synchronized void initCleaner() {
        if (!htmlCleanerInitialized) {
            cleaner = new HtmlCleaner();
            CleanerProperties props = cleaner.getProperties();
            props.setOmitComments(true);
            props.setOmitXmlDeclaration(true);
            htmlCleanerInitialized = true;
        }
    }
    
    /**
     * Extracts inner HTML of the tag which is first found by the <CODE>tagName</CODE>.
     * If <CODE>byHtmlCleaner</CODE> parameter is set to true, then HTML Cleaner library
     * will be used to extract the inner content of the tag found by the <CODE>tagName</CODE>.
     * <P>
     * You can use <CODE>byHtmlCleaner</CODE> option to extract complex html tags, but it
     * requires more operations because it needs html cleaning.
     * So, for simple html input and for better performance, you can extract tags with simple
     * extracting option by setting <CODE>byHtmlCleaner</CODE> to false.
     * If the html input is more complex and you need more correct result, then you need to set
     * <CODE>byHtmlCleaner</CODE> to true with more operational cost.
     * </P>
     * @param html
     * @param tagName
     * @param byHtmlCleaner
     * @return
     */
    public static String getInnerHtml(String html, String tagName, boolean byHtmlCleaner) {
        if (byHtmlCleaner) {
            return getInnerHtmlByCleaner(html, tagName);
        } else {
            return getInnerHtmlSimply(html, tagName);
        }
    }
    
    private static String getInnerHtmlSimply(String html, String tagName) {
        String tagInnerHtml = "";
        
        tagName = tagName.toUpperCase();
        String startTag = "<" + tagName;
        int startTagLen = startTag.length();
        String endTag = "</" + tagName + ">";
        
        boolean tagStarted = false;
        int offset = -1;

        StringReader sr = null;
        BufferedReader br = null;
        StringWriter sw = null;
        PrintWriter out = null;

        try {
            sr = new StringReader(html);
            br = new BufferedReader(sr);
            sw = new StringWriter(BUF_SIZE);
            out = new PrintWriter(sw);
            
            String line = br.readLine();

            while (line != null) {
                if (!tagStarted) {
                    offset = line.toUpperCase().indexOf(startTag);
                    
                    if (offset != -1) {
                        char ch = line.charAt(offset + startTagLen);
                        
                        if (ch == '>' || Character.isWhitespace(ch)) {
                            tagStarted = true;
                            offset = line.indexOf('>', offset + 6);
                            if (offset != -1) {
                                String temp = line.substring(offset + 1);
                                offset = temp.toUpperCase().indexOf(endTag);
                                if (offset == -1) {
                                    out.println(temp);
                                } else {
                                    out.println(temp.substring(0, offset));
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    offset = line.toUpperCase().indexOf(endTag);
                    
                    if (offset != -1) {
                        out.println(line.substring(0, offset));
                        break;
                    } else {
                        out.println(line);
                    }
                }
                
                line = br.readLine();
            }
            
            out.flush();
            tagInnerHtml = sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null) try { br.close(); } catch (Exception ce) { }
            if (sr != null) try { sr.close(); } catch (Exception ce) { }
            if (out != null) try { out.close(); } catch (Exception ce) { }
            if (sw != null) try { sw.close(); } catch (Exception ce) { }
        }
        
        return tagInnerHtml;
    }
    
    private static String getInnerHtmlByCleaner(String html, String tagName) {
        if (!htmlCleanerInitialized) {
            initCleaner();
        }
        
        String tagInnerHtml = null;
        
        if (html != null) {
            try {
                TagNode rootNode = cleaner.clean(html);
                TagNode [] targetNodes = rootNode.getElementsByName(tagName, true);
                
                if (targetNodes.length > 0) {
                    tagInnerHtml = cleaner.getInnerHtml(targetNodes[0]);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        return tagInnerHtml;
    }
    
}
