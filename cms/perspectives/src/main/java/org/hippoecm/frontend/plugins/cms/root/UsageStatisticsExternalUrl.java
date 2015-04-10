/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.root;

import java.util.Calendar;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.model.SystemInfoDataProvider;

public class UsageStatisticsExternalUrl {

    private static final String RELEASE_VERSION = "${RELEASE_VERSION}";
    private static final String YEAR = "${YEAR}";
    private static final String MONTH = "${MONTH}";
    private static final String DAY = "${DAY}";

    private static final String SERVER = "http://cdn.onehippo.com";
    private static final String PATH = "/cms-usage-statistics/" + RELEASE_VERSION + "/" + YEAR + "-" + MONTH + "-" + DAY + "/cms-usage-statistics.js";

    public static final String SYSTEM_PROPERTY_SERVER = "hippo.usage.statistics.server";

    private UsageStatisticsExternalUrl() {
    }

    private static String getServer() {
        final String server = System.getProperty(SYSTEM_PROPERTY_SERVER, SERVER);
        return server + PATH;
    }

    public static String get() {
        final SortedMap<String, String> parameters = new TreeMap<>();

        parameters.put(RELEASE_VERSION, new SystemInfoDataProvider().getReleaseVersion());

        final Calendar now = Calendar.getInstance();
        parameters.put(YEAR, String.valueOf(now.get(Calendar.YEAR)));
        parameters.put(MONTH, String.valueOf(now.get(Calendar.MONTH) + 1));
        parameters.put(DAY, String.valueOf(now.get(Calendar.DAY_OF_MONTH)));

        final String[] search = parameters.keySet().toArray(new String[parameters.size()]);
        final String[] replacements = parameters.values().toArray(new String[parameters.size()]);

        return StringUtils.replaceEach(getServer(), search, replacements);
    }

}
