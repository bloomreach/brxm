/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You
 *  may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.configuration;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.onehippo.cms7.services.hst.Channel;

public class RuntimeContextualizableMount extends RuntimeMount implements ContextualizableMount {

    private final ContextualizableMount delegatee;
    private final HstSite previewHstSite;

    public RuntimeContextualizableMount(final ContextualizableMount delegatee, final VirtualHost virtualHost) {
        this(delegatee, virtualHost, null);
    }
    public RuntimeContextualizableMount(final ContextualizableMount delegatee, final VirtualHost virtualHost, final Mount parent) {
        super(delegatee, virtualHost, parent);
        this.delegatee = delegatee;

        final HstSite delegateeSite = delegatee.getPreviewHstSite();
        if (delegateeSite != null) {
            previewHstSite = new RuntimeHstSite(delegateeSite, this);
        } else {
            previewHstSite = null;
        }
    }

    @Override
    public HstSite getPreviewHstSite() {
        return previewHstSite;
    }

    @Override
    public Channel getPreviewChannel() {
        if (previewHstSite == null) {
            return null;
        }
        return previewHstSite.getChannel();
    }

    @Override
    public <T extends ChannelInfo> T getPreviewChannelInfo() {
        return delegatee.getPreviewChannelInfo();
    }

    @Override
    public void addMount(final MutableMount mount) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Add mount is not supported for runtime mounts");
    }

    @Override
    public String toString() {
        return "RuntimeContextualizableMount{" +
                "delegatee=" + delegatee +
                '}';
    }
}
