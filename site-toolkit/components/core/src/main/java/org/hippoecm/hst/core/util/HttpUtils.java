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
package org.hippoecm.hst.core.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

/**
 * HttpUtils
 * 
 * @version $Id$
 */
public class HttpUtils {

    private HttpUtils() {

    }

    public static Map<String, String []> parseQueryString(HttpServletRequest request) {
        Map<String, String []> queryParamMap = null;

        String queryString = request.getQueryString();

        if (queryString == null) {
            queryParamMap = Collections.emptyMap();
        } else {
            queryParamMap = new HashMap<String, String []>();
            String[] paramPairs = StringUtils.split(queryString, '&');
            String paramName = null;
            
            for (String paramPair : paramPairs) {
                String[] paramNameAndValue = StringUtils.split(paramPair, '=');
                
                if (paramNameAndValue.length > 0) {
                    paramName = paramNameAndValue[0];
                    queryParamMap.put(paramName, null);
                }
            }
            
            for (Map.Entry<String, String []> entry : queryParamMap.entrySet()) {
                entry.setValue(request.getParameterValues(entry.getKey()));
            }
        }

        return queryParamMap;
    }

}
