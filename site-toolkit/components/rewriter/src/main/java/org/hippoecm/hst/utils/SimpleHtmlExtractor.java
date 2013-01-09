/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.io.IOUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple HTML Tag Extractor
 * 
 * @version $Id: SimpleHtmlExtractor.java 22564 2010-04-27 12:53:45Z wko $
 */
public class SimpleHtmlExtractor {

    private final static Logger log = LoggerFactory.getLogger(SimpleHtmlExtractor.class);
    
    private static final int BUF_SIZE = 512;
    
    private static boolean htmlCleanerInitialized;
    private static HtmlCleaner cleaner;
    
    private SimpleHtmlExtractor() {
    }
    
    private static synchronized void initCleaner() {
        if (!htmlCleanerInitialized) {
            cleaner = new HtmlCleaner();
            CleanerProperties props = cleaner.getProperties();
            props.setOmitComments(true);
            props.setOmitXmlDeclaration(true);
            htmlCleanerInitialized = true;
        }
    }
    
    protected static HtmlCleaner getHtmlCleaner() {
        if (!htmlCleanerInitialized) {
            initCleaner();
        }
        
        return cleaner;
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
     * <P>
     * If tagName is null or empty, then the root element is used.
     * </P>
     * @param html
     * @param tagName the name of the tag including the root or null/empty for root tag
     * @param byHtmlCleaner
     * @return String innerHTML of the tag or <code>null</code> when the tag is not found
     */
    public static String getInnerHtml(String html, String tagName, boolean byHtmlCleaner) {
        if (html == null) {
            return null;
        }
        
        if (byHtmlCleaner) {
            return getInnerHtmlByCleaner(html, tagName);
        } else {
            return getInnerHtmlSimply(html, tagName);
        }
    }
    
    /**
     * Extracts inner text of the tag which is first found by the <CODE>tagName</CODE>.
     * <P>
     * If tagName is null or empty, then the root element is used.
     * </P>
     * @param html
     * @param tagName the name of the tag including the root or null/empty for root tag
     * @return
     */
    public static String getInnerText(String html, String tagName) {
        if (html == null) {
            return null;
        }
        
        String innerText = "";
        
        TagNode targetNode = getTargetTagNode(html, tagName);
        
        if (targetNode != null) {
            innerText = targetNode.getText().toString();
        }
        
        return innerText;
    }
    
    /**
     * Extracts text of the html mark ups.
     * @param html
     * @return
     */
    public static String getText(String html) {
        if (html == null) {
            return null;
        }
        
        try {
            TagNode rootNode = getHtmlCleaner().clean(html);
            return rootNode.getText().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String getInnerHtmlSimply(String html, String tagName) {
        if (tagName == null || "".equals(tagName)) {
            tagName = "html";
        }
        
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
                            offset = line.indexOf('>', offset + tagName.length());
                            if (offset != -1) {
                                String temp = line.substring(offset + 1);
                                offset = temp.toUpperCase().indexOf(endTag);
                                if (offset == -1) {
                                    out.println(temp);
                                } else {
                                    out.print(temp.substring(0, offset));
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
            
            
            if (tagStarted)  {
                out.flush();
                return sw.toString();
            } 
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(sr);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(sw);
        }
        
        log.debug("Tag '{}' not found. Return null", tagName);
        
        return null;
    }
    
    private static String getInnerHtmlByCleaner(String html, String tagName) {
        String tagInnerHtml = null;
        
        if (html != null) {
            TagNode targetNode = getTargetTagNode(html, tagName);
            
            if (targetNode != null) {
                tagInnerHtml = getHtmlCleaner().getInnerHtml(targetNode);
            }
        }
        
        return tagInnerHtml;
    }
    
    private static TagNode getTargetTagNode(String html, String tagName) {
        TagNode targetNode = null;
        
        try {
            TagNode rootNode = getHtmlCleaner().clean(html);
            
            if (tagName == null || "".equals(tagName) || tagName.equalsIgnoreCase(rootNode.getName())) {
                return rootNode;
            }
            
            TagNode [] targetNodes = rootNode.getElementsByName(tagName, true);
            
            if (targetNodes.length > 0) {
                targetNode = targetNodes[0];
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return targetNode;
    }
    
}
