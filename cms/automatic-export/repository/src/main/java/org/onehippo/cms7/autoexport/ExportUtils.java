/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.autoexport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

final class ExportUtils {

    private ExportUtils() {}

    static String prefixFromName(String name) {
        // the name of the initialize item that imports cnd files
        // is either the namespace prefix corresponding to the nodetypes
        // in that cnd or or that namespace prefix postfixed by the string "-nodetypes"
        int offset = name.indexOf("-nodetypes");
        return offset == -1 ? name : name.substring(0, offset);
    }

    static List<String> getSubModuleExclusionPatterns(Configuration configuration, Module module) {
        List<String> subModuleExclusionPatterns = new ArrayList<String>();
        for (Map.Entry<String, Collection<String>> entry : configuration.getModules().entrySet()) {
            String modulePath = entry.getKey();
            Collection<String> repositoryPaths = entry.getValue();
            if (!modulePath.equals(module.getModulePath())) {
                for (String repositoryPath : repositoryPaths) {
                    if (module.isPathForModule(repositoryPath)) {
                        subModuleExclusionPatterns.addAll(Arrays.asList(repositoryPath, repositoryPath + "/**"));
                    }
                }
            }
        }
        return subModuleExclusionPatterns;
    }

    static boolean createFile(File file) throws IOException {
        File parentFile = file.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            return false;
        }
        return file.createNewFile();
    }

    /**
     * 1. Initialize item must be in the mapped module
     * 2. If there are multiple matching initialize items in the mapped module choose the first one that is enabled
     *
     * @param candidates initialize items that match the event path
     * @param module module that matches the module mapping
     * @return best matching initialize item or null if there were no initialize items in the list of candidates in the mapped module
     */
    static InitializeItem getBestMatchingInitializeItem(Collection<InitializeItem> candidates, Module module) {
        if (candidates == null) {
            return null;
        }
        InitializeItem result = null;
        for (InitializeItem item : candidates) {
            if (item.getModule().equals(module)) {
                if (result == null) {
                    result = item;
                } else {
                    if (item.isEnabled() && !result.isEnabled()) {
                        result = item;
                    }
                }
            }
        }
        return result;
    }

}
