/**
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import org.onehippo.cms7.services.hst.Channel;

public class PageCopyEventImpl extends BaseChannelEventImpl  implements PageCopyEvent {

    private static final long serialVersionUID = 1L;

    private final PageCopyContext pageCopyContext;

    public PageCopyEventImpl(final Channel channel, final PageCopyContext pageCopyContext) {
        super(channel);
        this.pageCopyContext = pageCopyContext;
    }

    @Override
    public PageCopyContext getPageCopyContext() {
        return pageCopyContext;
    }

    @Override
    public String toString() {
        return "PageCopyEventImpl{" +
                "pageCopyContext=" + getPageCopyContext() +
                "exception=" + getException() +
                '}';
    }

}
