/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standardworkflow.xpagelayout;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Provides the {@link XPageLayout}'s of the Channel identified by {@link #channelId}.
 * </p>
 */
public class HstXPageLayoutProvider implements XPageLayoutProvider {

    private static final Logger log = LoggerFactory.getLogger(HstXPageLayoutProvider.class);

    private final String channelId;

    public HstXPageLayoutProvider(final String channelId) {
        Objects.requireNonNull(channelId);
        this.channelId = channelId;
    }

    @Override
    public List<XPageLayout> getXPageLayouts() {

        final HstRequestContext hstRequestContext = RequestContextProvider.get();

        if (hstRequestContext == null) {
            log.warn("HstRequestContext expected to be present, cannot provide XPageLayouts if missing");
            return Collections.emptyList();
        }

        final ChannelService channelService = HippoServiceRegistry.getService(PlatformServices.class).getChannelService();

        final Map<String, XPageLayout> xPageLayouts = channelService.getXPageLayouts(channelId);

        if (xPageLayouts.isEmpty()) {
            log.info("No XPageLayouts found for channel id '{}', return empty xpage layouts", channelId);
            return Collections.emptyList();
        }

        return xPageLayouts.values().stream().collect(Collectors.toList());
    }


}
