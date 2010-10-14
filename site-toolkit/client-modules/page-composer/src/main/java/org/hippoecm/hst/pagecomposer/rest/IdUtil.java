/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.rest;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;

public class IdUtil {
    final static String SVN_ID = "$Id$";

    public static void savePath(String id, String path, HttpSession session) {
        getCache(session).put(id, path);
    }

    private static Map<String, String> getCache(HttpSession session) {
        Map<String, String> idCache = (Map<String, String>) session.getAttribute("idCache");
        if (idCache == null) {
            idCache = new HashMap<String, String>();
            session.setAttribute("idCache", idCache);
        }
        return idCache;
    }

    public static boolean containsPath(String id, HttpSession session) {
        return getCache(session).containsKey(id);
    }

    public static String getPath(String id, HttpSession http) {
        return getCache(http).get(id);
    }

    public static void remove(String id, HttpSession http) {
        getCache(http).remove(id);
    }

    public static boolean containsId(String path, HttpSession http) {
        return getCache(http).containsValue(path);
    }

    public static String getId(String path, HttpSession http) {
        for(Map.Entry<String, String> entry : getCache(http).entrySet()) {
            if(entry.getValue().equals(path)) {
                return entry.getKey();
            }
        }
        String id = DigestUtils.shaHex(path);
        getCache(http).put(id, path);
        return id;
    }
}
