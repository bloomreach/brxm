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
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsListPickerComponentInfo;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.onehippo.cms7.essentials.components.paging.Pageable;

/**
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsListPickerComponentInfo.class)
public class EssentialsListPickerComponent extends EssentialsListComponent {


    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {

        final EssentialsListPickerComponentInfo paramInfo = getComponentParametersInfo(request);
        final List<HippoDocument> documentItems = getDocumentItems(paramInfo);
        final int pageSize = getPageSize(request, paramInfo);
        final int page = getAnyIntParameter(request, REQUEST_PARAM_PAGE, 1);
        final Pageable<HippoDocument> pageable = new IterablePagination<>(documentItems, pageSize, page);
        request.setAttribute(REQUEST_ATTR_PAGEABLE, pageable);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);
    }


    public List<HippoDocument> getDocumentItems(final EssentialsListPickerComponentInfo componentInfo) {
        final List<HippoDocument> beans = new ArrayList<>();
        addBeanForPath(componentInfo.getDocumentItem1(), beans);
        addBeanForPath(componentInfo.getDocumentItem2(), beans);
        addBeanForPath(componentInfo.getDocumentItem3(), beans);
        addBeanForPath(componentInfo.getDocumentItem4(), beans);
        addBeanForPath(componentInfo.getDocumentItem5(), beans);
        addBeanForPath(componentInfo.getDocumentItem6(), beans);
        addBeanForPath(componentInfo.getDocumentItem7(), beans);
        addBeanForPath(componentInfo.getDocumentItem8(), beans);
        addBeanForPath(componentInfo.getDocumentItem9(), beans);
        addBeanForPath(componentInfo.getDocumentItem10(), beans);
        return beans;
    }
}
