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

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;

public class SimpleHmlStringParser {
    
    private static final SimpleHmlStringRewriter simpleHmlStringRewriter = new SimpleHmlStringRewriter();
    
    /**
     * @deprecated
     * @see {@link #parse(Node, String, HstRequest, HstResponse)}
     */
    public static String parse(Node node, String html, HttpServletRequest request, HstResponse response) {
        return parse(node, html, (HstRequest) request, response);
    }
    
    public static String parse(Node node, String html, HstRequest request, HstResponse response) {
        return simpleHmlStringRewriter.rewrite(html, node, request, response);
    }
    
    public static HstLink getLink(String path, Node node, HstRequest request, HstResponse response) {
        return simpleHmlStringRewriter.getLink(path, node, request, response);
    }
    
    public static boolean isExternal(String path) {
        return simpleHmlStringRewriter.isExternal(path);
    }
    
    private static class SimpleHmlStringRewriter extends SimpleContentRewriter {
        @Override
        public HstLink getLink(String path, Node node, HstRequest request, HstResponse response) {
            return super.getLink(path, node, request, response);
        }
        
        @Override
        public boolean isExternal(String path) {
            return super.isExternal(path);
        }
    }
    
}
