/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend;

import com.google.common.reflect.ClassPath;
import org.apache.wicket.resource.JQueryResourceReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LatestBundledJQueryResourceReference extends JQueryResourceReference {
    private static final long serialVersionUID = 1L;
    private static final String RESOURCE_PATH = "org/apache/wicket/resource/";
    private static final String JQUERY_FOLDER = "jquery/";

    private static final String VERSION_3 = "jquery/jquery-3.2.1.js";

    private String latestJQueryVersion;

    @Override
    public String getName() {
        return getLatestVersion();
    }

    public static String getVersion3() {
        return VERSION_3;
    }

    public String getLatestVersion() {
        if(latestJQueryVersion == null) latestJQueryVersion = findLatestVersion();
        return latestJQueryVersion;
    }

    protected String findLatestVersion() {
        try {
            // Find all resources in org/apache/wicket/resource/jquery. For Wicket include the jquery/ folder in the name
            List<String> bundledJQueryResources = new ArrayList<>();
            for (final ClassPath.ResourceInfo info : ClassPath.from(this.getClass().getClassLoader()).getResources()) {
                if (info.getResourceName().startsWith(RESOURCE_PATH.concat(JQUERY_FOLDER))) {
                    bundledJQueryResources.add(info.getResourceName().substring(RESOURCE_PATH.length()));
                }
            }

            // Filter out minified resources and get the latest JQuery version
            // This uses natural sorting so assumes no major JQuery version > 9 and only one minor version per major release
            return bundledJQueryResources.stream()
                    .filter(p -> p.endsWith((".min.js")))
                    .sorted(Comparator.reverseOrder()).collect(Collectors.toList()).get(0);
        } catch(Exception e) {
            // Fallback to the default version as defined in JQueryResourceReference
            return VERSION_1;
        }
    }
 }