/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to encode and decode node from and to urls
 * TODO: Add test cases: without them it's very tricky to get this exactly right
 */
public class PathUtils {

    public static String[] FULLY_QUALIFIED_URL_PREFIXES = {"//", "http:", "https:"};
    private static final Logger log = LoggerFactory.getLogger(PathUtils.class);

    private static final String HTML_SUFFIX = ".html";
    private static final String SLASH_ENCODED = "__slash__";
    
    private PathUtils() {
        
    }

    /**
     * remove trailing and leading slashes
     * @param path
     * @return normalized path which is the original path with all leading and trailing slashes removed
     */
    
    public static String normalizePath(String path) {
        if(path == null) {
            return null;
        } 
        while(path.startsWith("/")) {
           path = path.substring(1);
        }
        while(path.endsWith("/" )) {
            path = path.substring(0, path.length()-1);
        }
        return path;
    }

}

