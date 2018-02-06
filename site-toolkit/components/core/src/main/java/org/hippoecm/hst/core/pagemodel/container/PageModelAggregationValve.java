/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.pagemodel.container;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.AggregationValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.pagemodel.model.AggregatedPageModel;
import org.hippoecm.hst.core.pagemodel.model.ComponentContainerWindowModel;
import org.hippoecm.hst.core.pagemodel.model.ComponentWindowModel;
import org.hippoecm.hst.core.pagemodel.model.HippoBeanReferenceModel;
import org.hippoecm.hst.core.pagemodel.model.PageDefinitionModel;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PageModelAggregationValve extends AggregationValve {

    private static Logger log = LoggerFactory.getLogger(PageModelAggregationValve.class);

    private ObjectMapper objectMapper;

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void processWindowsRender(final HstContainerConfig requestContainerConfig,
            final HstComponentWindow[] sortedComponentWindows, final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {

        final HstRequestContext requestContext = RequestContextProvider.get();
        final HttpServletResponse response = requestContext.getServletResponse();

        PrintWriter writer = null;

        try {
            AggregatedPageModel pageModel = createAggregatedPageModel(sortedComponentWindows, requestMap);
            response.setContentType("application/json");
            writer = response.getWriter();
            writeAggregatedPageModel(writer, pageModel);
        } catch (IOException e) {
            log.warn("Failed to write aggregated page model in json.", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private void writeAggregatedPageModel(final Writer writer, final AggregatedPageModel pageModel)
            throws ContainerException, IOException {
        try {
            getObjectMapper().writeValue(writer, pageModel);
        } catch (JsonGenerationException e) {
            throw new ContainerException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new ContainerException(e.getMessage(), e);
        }
    }

    protected AggregatedPageModel createAggregatedPageModel(final HstComponentWindow[] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap) throws ContainerException {
        final AggregatedPageModel pageModel = new AggregatedPageModel();

        // root component (page component) is the first item in the sortedComponentWindows.
        PageDefinitionModel pageDefinition = new PageDefinitionModel(sortedComponentWindows[0].getComponentInfo().getId());
        pageModel.setPageDefinition(pageDefinition);

        ComponentContainerWindowModel curContainerWindow = null;

        // As sortedComponentWindows is sorted by parent-child order, we can assume all the container item component
        // window appears after a container component window.
        for (HstComponentWindow window : sortedComponentWindows) {
            if (window.isContainerWindow()) {
                curContainerWindow = new ComponentContainerWindowModel();
                curContainerWindow.setId(window.getReferenceNamespace());
                curContainerWindow.setName(window.getName());
                pageModel.addContainerWindow(curContainerWindow);
            } else if (window.isContainerItemWindow()) {
                if (curContainerWindow == null) {
                    log.warn("Invalid container item component window location for {}.", window.getReferenceNamespace());
                    continue;
                }

                final HstRequest hstRequest = requestMap.get(window);
                final ComponentWindowModel componentWindow = new ComponentWindowModel();
                componentWindow.setId(window.getReferenceNamespace());
                componentWindow.setName(window.getName());
                componentWindow.setType(window.getComponentName());
                componentWindow.setLabel(window.getComponentInfo().getLabel());

                for (Map.Entry<String, Object> entry : hstRequest.getModelsMap().entrySet()) {
                    final String name = entry.getKey();
                    final Object model = entry.getValue();

                    if (model instanceof HippoBean) {
                        final HippoBean beanModel = (HippoBean) model;
                        pageModel.putContent(beanModel.getCanonicalUUID(), beanModel);

                        final String contentRef = "#/content/" + beanModel.getIdentifier();
                        HippoBeanReferenceModel refModel = new HippoBeanReferenceModel(contentRef);
                        componentWindow.putModel(name, refModel);
                    } else {
                        componentWindow.putModel(name, model);
                    }
                }

                curContainerWindow.addComponentWindowSet(componentWindow);
            } else {
                curContainerWindow = null;
            }
        }

        return pageModel;
    }

}