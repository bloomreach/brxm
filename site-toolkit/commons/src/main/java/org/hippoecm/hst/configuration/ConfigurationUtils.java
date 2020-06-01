/*
 *  Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;

public class ConfigurationUtils {

    private static int[] supportedSchemeNotMatchingResponseCodes = {HttpServletResponse.SC_OK,
            HttpServletResponse.SC_MOVED_PERMANENTLY,
            HttpServletResponse.SC_MOVED_TEMPORARILY, HttpServletResponse.SC_SEE_OTHER, HttpServletResponse.SC_TEMPORARY_REDIRECT,
            HttpServletResponse.SC_FORBIDDEN, HttpServletResponse.SC_NOT_FOUND};

    public static boolean isSupportedSchemeNotMatchingResponseCode(int schemeNotMatchingResponseCode) {
        for (int code : supportedSchemeNotMatchingResponseCodes) {
            if (code == schemeNotMatchingResponseCode){
                return true;
            }
        }
        return false;
    }

    public static String supportedSchemeNotMatchingResponseCodesAsString() {
        StringBuilder builder = new StringBuilder();
        for (int code : supportedSchemeNotMatchingResponseCodes) {
            builder.append(code).append(", ");
        }
        return builder.substring(0, builder.length() -2).toString();
    }

    public static boolean isWorkspaceConfig(final HstNode node) {
        if (node == null) {
            return false;
        }
        if (HstNodeTypes.NODENAME_HST_WORKSPACE.equals(node.getName()) && HstNodeTypes.NODETYPE_HST_WORKSPACE.equals(node.getNodeTypeName())) {
            return true;
        }
        return isWorkspaceConfig(node.getParent());
    }

    public static boolean isValidContextPath(String path) {
        if (path == null) {
            // we allow context path to be null which means can be used to be
            // context path agnostic
            return true;
        }
        if (path.equals("")) {
            return true;
        }
        if (!path.startsWith("/")) {
            return false;
        }
        if (path.substring(1).contains("/")) {
            return false;
        }
        return true;
    }

    public static String createPrefixedParameterName(String prefix, String parameterName) {
        return prefix + HstComponentConfiguration.PARAMETER_PREFIX_NAME_DELIMITER + parameterName;
    }

}
