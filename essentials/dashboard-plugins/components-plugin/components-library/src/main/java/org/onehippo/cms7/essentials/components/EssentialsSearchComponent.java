/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsSearchComponentInfo;
import org.onehippo.cms7.essentials.components.paging.DefaultPagination;

import com.google.common.base.Strings;

/**
 * HST component used for searching of documents
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsSearchComponentInfo.class)
public class EssentialsSearchComponent extends EssentialsListComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        // execute only if valid query
        final String query = cleanupSearchQuery(getAnyParameter(request, REQUEST_PARAM_QUERY));
        if (Strings.isNullOrEmpty(query)) {
            request.setAttribute(REQUEST_ATTR_DOCUMENT, DefaultPagination.emptyCollection());
            return;
        }
        super.doBeforeRender(request, response);
    }

    @Override
    protected HippoBean getSearchScope(final HstRequest request, final String path) {
        final EssentialsSearchComponentInfo componentInfo = getComponentParametersInfo(request);
        return super.getSearchScope(request, componentInfo.getScope());
    }
}
