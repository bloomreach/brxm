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
package org.hippoecm.hst.core.mapping;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to encode and decode node from and to urls
 * TODO: Add test cases: without them it's very tricky to get this exactly right
 */
public class UrlUtilities {

    private static final Logger log = LoggerFactory.getLogger(UrlUtilities.class);
    private static final String HTML_SUFFIX = ".html";
    private static final String SLASH_ENCODED = "__slash__";

    /**
     * Encode the url:
     * <ul>
     *   <li>split url in url parts on '/'</li>
     *   <li>decode the jcr node name</li>
     *   <li>url encode the decoded name using utf-8</li>
     *   <li>append '.html' to the last url part</li>
     * </ul>
     * @param contextPath
     * @param uriLevels
     * @param rewrite
     * @return the encoded url
     */
    public static String encodeUrl(String contextPath,String rewrite) {
        int start = 0;
        if (rewrite.startsWith("/")) {
            // skip first part when starting with '/'
            start = 1;
        }
        String[] uriParts = rewrite.split("/");
        StringBuilder encodedUrl = new StringBuilder(contextPath);
        for (int i = start; i < uriParts.length - 1; i++) {
            encodedUrl.append("/").append(encodePart(uriParts[i]));
        }

        int last = uriParts.length - 1;
        if (last > 0) {
            if (uriParts[last].indexOf('/') > 0) {
                // the slash is the delimiter, needs extra care
                uriParts[last] = uriParts[last].replaceAll("\\/", SLASH_ENCODED);
            }
            /*
             * When the link is to a hippo document, the name coincides with the handle. 
             * If they do not contain a "." already, replace them by one part, and extend it by .html for nice urls
             */
            if (uriParts[last].equals(uriParts[last - 1]) && !uriParts[last].contains(".")) {
                encodedUrl.append(HTML_SUFFIX);
            } else {
                // for encoding a url, you have to decode the jcr node paths
                encodedUrl.append("/").append(encodePart(uriParts[last]));
            }
        }
        return encodedUrl.toString();
    }

    /**
     * 
     * Decode the url:
     * <ul>
     *   <li>split url in url parts on '/'</li>
     *   <li>url decode the url parts with utf-8</li>
     *   <li>jcr encode the decoded node name using utf-8</li>
     *   <li>append last url part twice for handle in document model</li>
     * </ul>
     * Always return at least a single slash
     * @param url
     * @return the decoded url starting with a slash
     */
    public static String decodeUrl(String url) {
        // quick handler for default cases
        if (url == null || "".equals(url) || "/".equals(url)) {
            return "/";
        }
        
        int start = 0;
        if (url.startsWith("/")) {
            // skip first empty uriPart
            start = 1;
        }

        String[] uriParts = url.split("/");
        StringBuilder decodedUrl = new StringBuilder();
        for (int i = start; i < uriParts.length - 1; i++) {
            decodedUrl.append("/").append(decodePart(uriParts[i]));
        }

        /*
         * if it ends with the html postfix and uriPart[i] != uriParts[i-1], it means we have to expand the request to
         *  /handle/document concept
         */
        int last = uriParts.length - 1;
        if (last > 0) {
            String lastPart = uriParts[last];
            if (lastPart.contains(SLASH_ENCODED)) {
                lastPart = lastPart.replaceAll(SLASH_ENCODED, "/");
            }
            if (lastPart.endsWith(HTML_SUFFIX) && !lastPart.equals(uriParts[last - 1])) {
                String name = decodePart(lastPart.substring(0, lastPart.length() - HTML_SUFFIX.length()));
                // add twice for handle
                decodedUrl.append("/").append(name);
                decodedUrl.append("/").append(name);
            } else {
                decodedUrl.append("/").append(decodePart(lastPart));
            }
        }
        return decodedUrl.toString();
    }

    /**
     * Helper method for encoding a single uri part
     * @param part
     * @return the jcr decoded and url encoded name
     */
    private static String encodePart(String part) {
        String name = NodeNameCodec.decode(part);
        try {
            if (name.indexOf('/') > 0) {
                // the slash is the delimiter, needs extra care
                return URLEncoder.encode(name.replaceAll("\\/", SLASH_ENCODED), "utf-8");
            } else {
                return URLEncoder.encode(name, "utf-8");
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Missing utf-8 codec?", e);
            return "";
        }
    }

    /**
     * Helper method for decoding a single uri part
     * @param part
     * @return the url decoded and jcr encoded part
     */
    private static String decodePart(String part) {
        try {
            String name = URLDecoder.decode(part, "utf-8");
            if (name.contains(SLASH_ENCODED)) {
                return NodeNameCodec.encode(name.replaceAll(SLASH_ENCODED, "/"));
            } else {
                return NodeNameCodec.encode(name);
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Missing utf-8 codec?", e);
            return "";
        }
    }

}
