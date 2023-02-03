/*
 * Copyright 2014-2023 Bloomreach
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

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HippoHtmlAdapter extends XmlAdapter<String, HippoHtml> {

    @Override
    public String marshal(HippoHtml html) throws Exception {
        if (html == null) {
            return null;
        }
        final HstRequestContext context = RequestContextProvider.get();
        return new SimpleContentRewriter().rewrite(html.getContent(), html.getNode(), context);
    }

    @Override
    public HippoHtml unmarshal(String representation) throws Exception {
        throw new UnsupportedOperationException("Unmarshalling not implemented.");
    }
}
