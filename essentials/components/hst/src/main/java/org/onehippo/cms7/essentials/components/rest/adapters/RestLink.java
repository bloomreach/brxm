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

package org.onehippo.cms7.essentials.components.rest.adapters;

import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "link")
public class RestLink {


    private String path;

    public RestLink() {
    }

    public RestLink(final HippoBean bean) {
        this.path = bean.getCanonicalPath();
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getLink() {
        final HstRequestContext context = RequestContextProvider.get();
        return context.getHstLinkCreator().create("/", context.getResolvedMount().getMount()).toUrlForm(context, true);
    }


}
