/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.container.header.AccessControlAllowHeadersService;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;

import static java.util.Collections.singletonList;

public class AccessControlAllowHeadersServiceImpl implements AccessControlAllowHeadersService {

    private final Map<String, List<String>> allowedHeadersMap = new HashMap<>();
    private String extraAllowedHeaders;

    public void setExtraAllowedHeaders(final String extraAllowedHeaders) {
        this.extraAllowedHeaders = extraAllowedHeaders;
    }

    public void init() {

        allowedHeadersMap.put(AccessControlAllowHeadersServiceImpl.class.getName() + ".builtin",
                singletonList(ContainerConstants.PAGE_MODEL_ACCEPT_VERSION));

        if (StringUtils.isNotBlank(extraAllowedHeaders)) {
            String[] split = StringUtils.split(extraAllowedHeaders, " ,\t\f\r\n");

            final List<String> extraAllowedHeaders = Arrays.stream(split).map(s -> s.trim())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

            allowedHeadersMap.put(AccessControlAllowHeadersServiceImpl.class.getName() + ".extra",
                    Collections.unmodifiableList(extraAllowedHeaders));
        }


        HippoServiceRegistry.register(this, AccessControlAllowHeadersService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, AccessControlAllowHeadersService.class);
    }

    @Override
    public String getAllowedHeadersString() {
        return allowedHeadersMap.values().stream()
                .flatMap(Collection::stream)
                .filter(StringUtils::isNotBlank)
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
        return allowedHeadersMap.get(module);
    }

    @Override
    public void setAllowedHeaders(final String module, final List<String> allowedHeaders) {
        allowedHeadersMap.put(module, Collections.unmodifiableList(allowedHeaders));
    }

}
