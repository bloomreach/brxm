/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsCarouselComponentInfo;
import org.onehippo.cms7.essentials.components.paging.DefaultPagination;

/**
 * HST component used for rendering of Carousel items
 */
@ParametersInfo(type = EssentialsCarouselComponentInfo.class)
public class EssentialsCarouselComponent extends CommonComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        setComponentId(request, response);

        final EssentialsCarouselComponentInfo paramInfo = getComponentParametersInfo(request);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);

        final List<String> carouselItemsStrings = getCarouselItemStrings(paramInfo);
        final List<HippoDocument> items = getCarouselItems(carouselItemsStrings);
        request.setModel(REQUEST_ATTR_PAGEABLE, new DefaultPagination<>(items));

        final Map<Boolean, List<Integer>> configuredItemNumbers = getConfiguredItemNumbers(carouselItemsStrings);
        request.setAttribute("configuredItems", configuredItemNumbers.get(true));
        request.setAttribute("freeItems", configuredItemNumbers.get(false));
    }

    /**
     * Populates a list of carousel documents.
     *
     * @param componentInfo Carousel component annotation
     * @return list of documents to be populated
     * @deprecated use @{link {@link #getCarouselItems(List)}} instead
     */
    @Deprecated
    public List<HippoDocument> getCarouselItems(final EssentialsCarouselComponentInfo componentInfo) {
        final List<String> carouselItemsStrings = getCarouselItemStrings(componentInfo);
        return getCarouselItems(carouselItemsStrings);
    }

    /**
     * Populates a list of carousel documents.
     *
     * @param carouselItemsStrings the defined item strings
     * @return list of documents to be populated
     */
    public List<HippoDocument> getCarouselItems(final List<String> carouselItemsStrings) {
        return carouselItemsStrings.stream()
                .map(c -> getHippoBeanForPath(c, HippoDocument.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Populates two lists of items that are configured and are not configured.
     * 
     * @param carouselItemStrings the item strings
     * @return a map with two lists
     */
    private Map<Boolean, List<Integer>> getConfiguredItemNumbers(final List<String> carouselItemStrings) {
        return IntStream.rangeClosed(1, 6)
                .boxed()
                .collect(Collectors.partitioningBy(i -> StringUtils.isNotBlank(carouselItemStrings.get(i - 1))));
    }

    /**
     * Put the six separate configured items in a List.
     *
     * @param componentInfo Carousel component annotation
     * @return list of carousel item paths
     */
    public List<String> getCarouselItemStrings(final EssentialsCarouselComponentInfo componentInfo) {
        final List<String> carouselItems = new ArrayList<>(6);
        carouselItems.add(componentInfo.getCarouselItem1());
        carouselItems.add(componentInfo.getCarouselItem2());
        carouselItems.add(componentInfo.getCarouselItem3());
        carouselItems.add(componentInfo.getCarouselItem4());
        carouselItems.add(componentInfo.getCarouselItem5());
        carouselItems.add(componentInfo.getCarouselItem6());
        return carouselItems;        
    }
}
