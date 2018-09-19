/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;

public class BeforeChannelDeleteEventImpl extends BaseChannelEventImpl implements BeforeChannelDeleteEvent {

    private static final long serialVersionUID = 1L;

    private transient final HstRequestContext requestContext;
    private final List<Mount> mounts;

    public BeforeChannelDeleteEventImpl(final Channel channel, final List<Mount> mountsOfChannel, final HstRequestContext requestContext) {
        super(channel);
        this.requestContext = requestContext;
        this.mounts = new ArrayList<>();

        if (mountsOfChannel != null) {
            this.mounts.addAll(mountsOfChannel);
        }
    }

    /**
     * Return all mounts binding to the deleting channel
     */
    @Override
    public List<Mount> getMounts() {
        if (mounts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(mounts);
    }

    @Override
    public HstRequestContext getRequestContext() {
        return requestContext;
    }

    @Override
    public String toString() {
        return "BeforeChannelDeleteEventImpl{" +
                ", channel=" + getChannel() +
                ", exception=" + getException() +
                '}';
    }

}
