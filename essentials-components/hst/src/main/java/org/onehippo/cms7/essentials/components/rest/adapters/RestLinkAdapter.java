/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.rest.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.hippoecm.hst.content.beans.standard.HippoDocument;

public class RestLinkAdapter extends XmlAdapter<RestLink, HippoDocument> {

    @Override
    public RestLink marshal(HippoDocument bean) throws Exception {
        if (bean == null) {
            return null;
        }
        return new RestLink(bean);
    }

    @Override
    public HippoDocument unmarshal(RestLink representation) throws Exception {
        throw new UnsupportedOperationException("Unmarshalling not implemented.");
    }

}
