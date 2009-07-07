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

/**
 * Simple HTML Tag Extractor
 * 
 * @version $Id$
 */
public class SimpleHtmlExtractor {
    
    private SimpleHtmlExtractor() {
    }

    /**
     * Extracts inner HTML of the tag which is first found by the tagName.
     * @param html
     * @param tagName
     * @return
     */
    public static String getTagInnerContent(String html, String tagName) {
        String tagInnerContent = "";
        
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
            sw = new StringWriter(512);
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
            tagInnerContent = sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null) try { br.close(); } catch (Exception ce) { }
            if (sr != null) try { sr.close(); } catch (Exception ce) { }
            if (out != null) try { out.close(); } catch (Exception ce) { }
            if (sw != null) try { sw.close(); } catch (Exception ce) { }
        }
        
        return tagInnerContent;
    }
    
}
