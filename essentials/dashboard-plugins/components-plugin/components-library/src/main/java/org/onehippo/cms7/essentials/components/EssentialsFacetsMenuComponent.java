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

package org.onehippo.cms7.essentials.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsFacetsMenuComponentInfo;
import org.onehippo.cms7.essentials.components.utils.SiteUtils;

/**
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsFacetsMenuComponentInfo.class)
public class EssentialsFacetsMenuComponent extends EssentialsFacetsComponent {


    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsFacetsMenuComponentInfo componentInfo = getComponentParametersInfo(request);
        final String facetPath = componentInfo.getFacetPath();

        final HippoFacetNavigationBean facNavBean = getFacetNavigationBean(request, facetPath, null);
        if (facNavBean == null) {
            return;
        }
        Collection<HippoFolderBean> facetFolders = new ArrayList<>();
        final Set<String> facetNames = SiteUtils.parseCommaSeparatedValueAsSet(componentInfo.getFacetNames());
        final boolean filter = facetNames.size() > 0;
        for (HippoFolderBean hippoFolderBean : facNavBean.getFolders()) {
            if (filter && facetNames.contains(hippoFolderBean.getName())) {
                facetFolders.add(hippoFolderBean);
            } else {
                facetFolders.add(hippoFolderBean);
            }
        }

        request.setAttribute("facetFolders", facetFolders);
    }


}
