/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import org.hippoecm.hst.configuration.hosting.Mount;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CHANNEL;

public class ChannelHelper extends AbstractHelper {

    @Override
    public Object getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public Object getConfigObject(final String itemId, final Mount mount) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    protected String getNodeType() {
        return NODETYPE_HST_CHANNEL;
    }

}
