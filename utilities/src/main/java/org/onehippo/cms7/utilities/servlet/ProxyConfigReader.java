/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;

import org.apache.commons.lang.StringUtils;

class ProxyConfigReader {

    private static final String PROXY_FROM_TO_SEPARATOR = "@";
    private static final String PROXY_SEPARATOR = ",";

    private ProxyConfigReader() {
    }

    /**
     * Example configurations
     * <p></p>
     * jarPath = /angular<br/>
     * from = angular/hippo-cm<br/>
     * to = http://localhost:9090<br/>
     * Results in mapping "/hippo-cm/" -> "http://localhost:9090/"
     * <p></p>
     * jarPath = /angular<br/>
     * from = angular/hippo-cm<br/>
     * to = http://localhost:9090/cms/angular/hippo-cm
     * <p>
     * Results in mapping "/hippo-cm/" -> "http://localhost:9090/cms/angular/hippo-cm/"
     */
    static Set<ProxyConfig> getProxies(final String jarPathPrefix, final String proxiesAsString) {
        if (StringUtils.isBlank(proxiesAsString)) {
            return Collections.emptySet();
        }

        return Arrays.stream(StringUtils.split(proxiesAsString, PROXY_SEPARATOR))
                .filter(StringUtils::isNotBlank)
                .map(proxyLine -> {
                    final String[] fromTo = StringUtils.split(proxyLine, PROXY_FROM_TO_SEPARATOR);
                    if (fromTo.length <= 1 || StringUtils.isBlank(fromTo[0]) || StringUtils.isBlank(fromTo[1])) {
                        return null;
                    }

                    String from = fromTo[0].trim();
                    from = prependSlashIfMissing(from);
                    from = appendSlashIfMissing(from);
                    if (!from.startsWith(jarPathPrefix + "/")) {
                        return null;
                    }
                    from = StringUtils.removeStart(from, jarPathPrefix);

                    String to = fromTo[1].trim();
                    to = appendSlashIfMissing(to);

                    return new ProxyConfig(from, to);
                })
                .filter(Objects::nonNull)
                .collect(ProxyConfigReader.toImmutableSet());
    }

    private static String prependSlashIfMissing(final String str) {
        return StringUtils.startsWith(str, "/") ? str : "/" + str;
    }

    private static String appendSlashIfMissing(final String str) {
        return StringUtils.endsWith(str, "/") ? str : str + "/";
    }

    private static <T> Collector<T, Set<T>, Set<T>> toImmutableSet() {
        return Collector.of(HashSet::new, Set::add, (l, r) -> {
            l.addAll(r);
            return l;
        }, Collections::unmodifiableSet);
    }
}
