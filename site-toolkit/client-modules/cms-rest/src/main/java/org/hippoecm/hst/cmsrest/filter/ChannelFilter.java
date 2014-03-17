/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.cmsrest.filter;

import java.util.List;

import com.google.common.base.Predicate;

import org.hippoecm.hst.configuration.channel.Channel;

public class ChannelFilter implements Predicate<Channel> {

    private List<Predicate> filters;

    public void setFilters(final List<Predicate> filters) {
        this.filters = filters;
    }

    @Override
    public boolean apply(final org.hippoecm.hst.configuration.channel.Channel channel) {
        if (filters == null) {
            return true;
        }
        for (Predicate filter : filters) {
            boolean predicate = filter.apply(channel);
            if (!predicate) {
                return false;
            }
        }
        return true;
    }
}
