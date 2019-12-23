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
package org.hippoecm.hst.platform.services;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.container.header.AccessControlAllowHeadersService;
import org.onehippo.cms7.services.HippoServiceRegistry;

public class AccessControlAllowHeadersServiceImpl implements AccessControlAllowHeadersService {

    public void init() {
        HippoServiceRegistry.register(this, AccessControlAllowHeadersService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, AccessControlAllowHeadersService.class);
    }

    private final Map<String, List<String>> allowedHeadersMap = new HashMap();

    @Override
    public String getAllowedHeadersString() {
        return allowedHeadersMap.values().stream()
                .flatMap(Collection::stream)
                .filter(s -> StringUtils.isNotBlank(s))
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    @Override
    public Map<String, List<String>> getAllAllowedHeaders() {
        return Collections.unmodifiableMap(allowedHeadersMap);
    }

    @Override
    public List<String> getAllowedHeaders(final String module) {
        final List<String> list = allowedHeadersMap.get(module);
        if (list == null) {
            return null;
        }
        return Collections.unmodifiableList(list);
    }


    @Override
    public void setAllowedHeaders(final String module, final List<String> allowedHeaders) {
        allowedHeadersMap.put(module, Collections.unmodifiableList(allowedHeaders));
    }
}
