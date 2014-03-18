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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsCarouselComponentInfo;
import org.onehippo.cms7.essentials.components.paging.DefaultPagination;

/**
 * HST component used for rendering of Carousel items
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsCarouselComponentInfo.class)
public class EssentialsCarouselComponent extends CommonComponent {


    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsCarouselComponentInfo componentInfo = getComponentParametersInfo(request);
        final List<HippoDocument> items = getCarouselItems(request, componentInfo);
        request.setAttribute(REQUEST_PARAM_PAGEABLE, new DefaultPagination<>(items));
        setCarouselOptions(request, componentInfo);
    }

    /**
     * Sets options like effects, speed, etc. of selected carousel component
     *
     * @param request       HstRequest instance
     * @param componentInfo Carousel component annotation
     */
    public void setCarouselOptions(final HstRequest request, final EssentialsCarouselComponentInfo componentInfo) {
        request.setAttribute("displayTime", componentInfo.displayTime());
        request.setAttribute("fx", componentInfo.fx());
        request.setAttribute("showNavigation", componentInfo.showNavigation());
    }

    /**
     * Populates a list of carousel documents
     *
     * @param request       HstRequest instance
     * @param componentInfo Carousel component annotation
     * @return list of documents to be populated
     */
    public List<HippoDocument> getCarouselItems(final HstRequest request, final EssentialsCarouselComponentInfo componentInfo) {
        final List<HippoDocument> beans = new ArrayList<>();
        addBeanForPath(request, componentInfo.getCarouselItem1(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem2(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem3(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem4(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem5(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem6(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem7(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem8(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem9(), beans);
        addBeanForPath(request, componentInfo.getCarouselItem10(), beans);
        return beans;
    }


}
