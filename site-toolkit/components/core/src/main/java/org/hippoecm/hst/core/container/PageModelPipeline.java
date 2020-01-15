/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageModelPipeline implements Pipeline {

    protected final static Logger log = LoggerFactory.getLogger(PageModelPipeline.class);

    private final static String PAGE_MODEL_PIPELINE_REQUEST_ATTR = PageModelPipeline.class.getName() + ".pageModelPipeline";

    private String defaultPageModelApiVersion;

    private Map<String, Pipeline> pageModelApiPipelinesByVersion = new HashMap<>();

    protected List<Valve> extraInitValves = new ArrayList<>();
    protected List<Valve> extraProcessingValves = new ArrayList<>();
    protected List<Valve> extraCleanupValves = new ArrayList<>();

    private volatile boolean initialized = false;

    @SuppressWarnings("unused")
    public void addPageModelApiPipelineByVersion(final String version, final Pipeline pipeline) {
        pageModelApiPipelinesByVersion.put(version, pipeline);
    }


    public void setDefaultPageModelApiVersion(String defaultPageModelApiVersion) {
        this.defaultPageModelApiVersion = defaultPageModelApiVersion;
    }

    /**
     * Used by downstream projects to be able to add a valve to all delegatee pageModelApiPipelinesByVersion pipelines
     */
    @SuppressWarnings("unused")
    public void addInitializationValve(Valve initializationValve) {
        extraInitValves.add(initializationValve);
    }

    /**
     * Used by downstream projects to be able to add a valve to all delegatee pageModelApiPipelinesByVersion pipelines
     */
    @SuppressWarnings("unused")
    public void addProcessingValve(Valve processingValve) {
        extraProcessingValves.add(processingValve);
    }

    /**
     * Used by downstream projects to be able to add a valve to all delegatee pageModelApiPipelinesByVersion pipelines
     */
    @SuppressWarnings("unused")
    public void addCleanupValve(Valve cleanupValve) {
        extraCleanupValves.add(cleanupValve);
    }


    @Override
    public void initialize() throws ContainerException {
    }

    @Override
    public void invoke(final HstContainerConfig requestContainerConfig, final HstRequestContext requestContext,
                       final HttpServletRequest servletRequest, final HttpServletResponse servletResponse) throws ContainerException {
        if (!initialized) {
            doInitialize();
        }
        try {
            getPageModelPipelineDelegatee(servletRequest).invoke(requestContainerConfig, requestContext, servletRequest, servletResponse);
        } catch (UnsupportedApiVersion e) {
            try {
                servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } catch (IOException e1) {
                throw new ContainerException("Unable to set 400 on response after invalid request version.", e);
            }
        }
    }

    /**
     * Note that we cannot use to call {@link #initialize()} via spring init-method on spring bean because the spring bean
     * is created before downsteam projects add possibly extra valves
     */
    private synchronized void doInitialize() {
        if (initialized) {
            return;
        }
        // process all extra set valves for all pipelines we have
        for (Pipeline pipeline : pageModelApiPipelinesByVersion.values()) {
            if (pipeline instanceof HstSitePipeline) {
                final HstSitePipeline hstSitePipeline = (HstSitePipeline) pipeline;
                extraInitValves.forEach(valve -> hstSitePipeline.addInitializationValve(valve));
                extraProcessingValves.forEach(valve -> hstSitePipeline.addProcessingValve(valve));
                extraCleanupValves.forEach(valve -> hstSitePipeline.addCleanupValve(valve));
            }
        }
        initialized = true;
    }

    @Override
    public void cleanup(final HstContainerConfig requestContainerConfig, final HstRequestContext requestContext,
                        final HttpServletRequest servletRequest, final HttpServletResponse servletResponse) throws ContainerException {
        try {
            getPageModelPipelineDelegatee(servletRequest).cleanup(requestContainerConfig, requestContext, servletRequest, servletResponse);
        } catch (UnsupportedApiVersion ignore) {
            // already handled during #invoke
        }
    }

    @Override
    public void destroy() throws ContainerException {
    }

    /**
     * <p>
     *     Returns the {@link Pipeline} to delegate to. The selected {@link Pipeline} corresponds to the requested
     *     Page Model API version. If there is no pipeline for the requested version, an {@link UnsupportedApiVersion}
     *     is thrown
     * </p>
     * @param servletRequest
     * @return The {@link Pipeline} to delegate to
     * @throws UnsupportedApiVersion in case of an unsupported Page Model API is requested
     */
    private Pipeline getPageModelPipelineDelegatee(final HttpServletRequest servletRequest) throws UnsupportedApiVersion {
        final Pipeline pipeline = (Pipeline)servletRequest.getAttribute(PAGE_MODEL_PIPELINE_REQUEST_ATTR);
        if (pipeline != null) {
            return pipeline;
        }
        final String requestPageModelApiVersion = servletRequest.getHeader(ContainerConstants.PAGE_MODEL_ACCEPT_VERSION);
        if (StringUtils.isEmpty(requestPageModelApiVersion)) {
            final Pipeline defaultPipeline = getDefaultPageModelPipeline();
            servletRequest.setAttribute(ContainerConstants.PAGE_MODEL_API_VERSION, defaultPageModelApiVersion);
            servletRequest.setAttribute(PAGE_MODEL_PIPELINE_REQUEST_ATTR, defaultPipeline);
            return defaultPipeline;
        } else {
            final Pipeline requestedPipeline = pageModelApiPipelinesByVersion.get(requestPageModelApiVersion);
            if (requestedPipeline == null) {
                throw new UnsupportedApiVersion(String.format("UnsupportedApiVersion: Header 'Accept-version: %s' points " +
                        "to a non-supported version", requestPageModelApiVersion));
            } else {
                log.info("Using page model api pipeline version '{}'", requestPageModelApiVersion);
                servletRequest.setAttribute(ContainerConstants.PAGE_MODEL_API_VERSION, requestPageModelApiVersion);
                servletRequest.setAttribute(PAGE_MODEL_PIPELINE_REQUEST_ATTR, requestedPipeline);
                return requestedPipeline;
            }
        }
    }

    private Pipeline getDefaultPageModelPipeline() throws UnsupportedApiVersion {
        final Pipeline defaultPipeline = pageModelApiPipelinesByVersion.get(defaultPageModelApiVersion);
        if (defaultPipeline == null) {
            throw new UnsupportedApiVersion(String.format("Default Page Model API version '%s' is not supported.",
                    defaultPageModelApiVersion));
        }
        return defaultPipeline;
    }

    private static class UnsupportedApiVersion extends Exception {
        public UnsupportedApiVersion(final String message) {
            super(message);
        }
    }
}
