/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProxyConfigReader {

    private static final Logger log = LoggerFactory.getLogger(ProxyConfigReader.class);
    private static final String PROXY_FROM_TO_SEPARATOR = "@";
    private static final String PROXY_SEPARATOR = ",";
    private static final String PROXY_ENABLED_MESSAGE = "Resource proxy enabled with the following mapping(s)";
    private final String jarPathPrefix;
    private final String proxiesAsString;
    private final Map<String, String> proxies = new HashMap<>();

    public ProxyConfigReader(final String jarPathPrefix, final String proxiesAsString) {
        this.jarPathPrefix = jarPathPrefix;
        this.proxiesAsString = proxiesAsString;
        initProxies();
        if (!proxies.isEmpty()) {
            final List<String> messages = new ArrayList<>(proxies.size() + 1);
            messages.add(PROXY_ENABLED_MESSAGE);
            proxies.forEach((from, to) -> messages.add(String.format("from %s to %s", from, to)));
            logBorderedMessage(messages);
        }

    }

    private static String prependIfMissing(final String str) {
        if (str == null || StringUtils.isEmpty("/") || StringUtils.startsWith(str, "/")) {
            return str;
        }
        return "/" + str;
    }

    private static String appendIfMissing(final String str) {
        if (str == null || StringUtils.isEmpty("/") || StringUtils.endsWith(str, "/")) {
            return str;
        }
        return str + "/";
    }

    private static void logBorderedMessage(final List<String> messages) {
        final String prefix = "** ";
        final String suffix = " **";
        final int fixLength = prefix.length() + suffix.length();

        // find longest message
        messages.stream().max(Comparator.comparingInt(String::length)).ifPresent(longest -> {
            final int width = longest.length() + fixLength;
            final String border = StringUtils.repeat("*", width);
            log.info(border);
            messages.forEach(msg -> {
                final String rightPadding = StringUtils.repeat(" ", width - fixLength - msg.length());
                log.info(prefix + msg + rightPadding + suffix);
            });
            log.info(border);
        });
    }

    public Map<String, String> getProxies() {
        return new HashMap<>(proxies);
    }

    /**
     * Examples configurations and mapping - jarPath=/angular - from=angular/hippo-cm - to=http://localhost:9090 Results
     * in mapping "/hippo-cm/" -> "http://localhost:9090/"
     * <p>
     * - jarPath=/angular - from=angular/hippo-cm - to=http://localhost:9090/cms/angular/hippo-cm Results in mapping
     * "/hippo-cm/" -> "http://localhost:9090/cms/angular/hippo-cm/"
     */
    private void initProxies() {
        if (StringUtils.isNotBlank(proxiesAsString)) {
            Arrays.stream(StringUtils.split(proxiesAsString, PROXY_SEPARATOR))
                    .filter(StringUtils::isNotBlank)
                    .map(proxyLine -> StringUtils.split(proxyLine, PROXY_FROM_TO_SEPARATOR))
                    .forEach(fromTo -> {
                        if (fromTo.length > 1) {
                            String from = StringUtils.trim(fromTo[0]);
                            from = prependIfMissing(from);
                            from = appendIfMissing(from);
                            String to = StringUtils.trim(fromTo[1]);
                            to = appendIfMissing(to);
                            if (from.startsWith(jarPathPrefix + "/")) {
                                from = StringUtils.removeStart(from, jarPathPrefix);
                                proxies.put(from, to);
                            }
                        }
                    });
        }
    }
}
