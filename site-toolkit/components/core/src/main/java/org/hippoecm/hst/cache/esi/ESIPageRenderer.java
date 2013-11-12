/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.esi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.james.mime4j.util.MimeUtil;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerConfigImpl;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.util.PropertyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.constructs.web.GenericResponseWrapper;

/**
 * ESIPageRenderer
 */
public class ESIPageRenderer implements ComponentManagerAware {

    private static Logger log = LoggerFactory.getLogger(ESIPageRenderer.class);

    private HstManager hstManager;
    private ComponentManager componentManager;
    private String esiIncludePipelineName;
    private HstContainerConfig requestContainerConfig;

    public ESIPageRenderer() {
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public void setHstManager(HstManager hstManager) {
        this.hstManager = hstManager;
    }

    public void setEsiIncludePipelineName(String esiIncludePipelineName) {
        this.esiIncludePipelineName = esiIncludePipelineName;
    }

    public void render(Writer writer, HttpServletRequest request, ESIHstPageInfo pageInfo) {
        HstRequestContext requestContext = RequestContextProvider.get();

        PropertyParser propertyParser = createESIPropertyParser(request);

        if (requestContainerConfig == null && requestContext != null) {
            requestContainerConfig = new HstContainerConfigImpl(requestContext.getServletContext(), Thread.currentThread().getContextClassLoader());
        }

        String bodyContent = pageInfo.getUngzippedBodyAsString();
        List<ESIFragmentInfo> fragmentInfos = pageInfo.getFragmentInfos();

        if (fragmentInfos.isEmpty()) {
            writeQuietly(writer, bodyContent);
            return;
        }

        int beginIndex = 0;

        for (ESIFragmentInfo fragmentInfo : fragmentInfos) {
            ESIFragment fragment = fragmentInfo.getFragment();
            ESIFragmentType type = fragment.getType();

            writeQuietly(writer, bodyContent.substring(beginIndex, fragmentInfo.getBeginIndex()));
            beginIndex = fragmentInfo.getEndIndex();

            if (type == ESIFragmentType.COMMENT_BLOCK) {
                String uncommentedSource = fragment.getSource();

                if (!((ESICommentFragmentInfo) fragmentInfo).hasAnyFragmentInfo()) {
                    writeQuietly(writer, uncommentedSource);
                } else {
                    List<ESIFragmentInfo> embeddedFragmentInfos = ((ESICommentFragmentInfo) fragmentInfo).getFragmentInfos();
                    int embeddedBeginIndex = 0;

                    for (ESIFragmentInfo embeddedFragmentInfo : embeddedFragmentInfos) {
                        ESIFragment embeddedFragment = embeddedFragmentInfo.getFragment();
                        ESIFragmentType embeddedFragmentType = embeddedFragment.getType();

                        writeQuietly(writer, uncommentedSource.substring(embeddedBeginIndex, embeddedFragmentInfo.getBeginIndex()));
                        embeddedBeginIndex = embeddedFragmentInfo.getEndIndex();

                        if (embeddedFragmentType == ESIFragmentType.INCLUDE_TAG) {
                            ESIElementFragment embeddedElementFragment = (ESIElementFragment) embeddedFragment;
                            String onerror = embeddedElementFragment.getElement().getAttribute("onerror");

                            if (StringUtils.isNotEmpty(onerror) && !StringUtils.equals("continue", onerror)) {
                                log.warn("The onerror attribute of <esi:include/> currently support only 'continue'. Other values ('{}') are NOT YET SUPPORTED.", onerror);
                            }

                            try {
                                writeIncludeElementFragment(writer, embeddedElementFragment, propertyParser);
                            } catch (IOException e) {
                                if (!StringUtils.equals("continue", onerror)) {
                                    //
                                }
                            }
                        }
                    }

                    writeQuietly(writer, uncommentedSource.substring(embeddedFragmentInfos.get(embeddedFragmentInfos.size() - 1).getEndIndex()));
                }
            } else if (type == ESIFragmentType.INCLUDE_TAG) {
                ESIElementFragment elementFragment = (ESIElementFragment) fragment;
                String onerror = elementFragment.getElement().getAttribute("onerror");

                if (StringUtils.isNotEmpty(onerror) && !StringUtils.equals("continue", onerror)) {
                    log.warn("The onerror attribute of <esi:include/> currently support only 'continue'. Other values ('{}') are NOT YET SUPPORTED.", onerror);
                }

                try {
                    writeIncludeElementFragment(writer, elementFragment, propertyParser);
                } catch (IOException e) {
                    if (!StringUtils.equals("continue", onerror)) {
                        //
                    }
                }
            } else if (type == ESIFragmentType.VARS_TAG) {
                writeNonIncludeElementFragment(writer, (ESIElementFragment) fragment, propertyParser);
            }
        }

        writeQuietly(writer, bodyContent.substring(fragmentInfos.get(fragmentInfos.size() - 1).getEndIndex()));
    }

    protected void writeNonIncludeElementFragment(Writer writer, ESIElementFragment fragment, PropertyParser propertyParser) {
        ESIFragmentType type = fragment.getType();

        if (type == ESIFragmentType.VARS_TAG) {
            writeVarsElementFragment(writer, fragment, propertyParser);
        }
    }

    protected void writeIncludeElementFragment(Writer writer, ESIElementFragment fragment, PropertyParser propertyParser) throws IOException {
        String src = fragment.getElement().getAttribute("src");
        String alt = fragment.getElement().getAttribute("alt");

        if (src != null) {
            src = (String) propertyParser.resolveProperty(getClass().getSimpleName(), src);
        }

        if (alt != null) {
            alt = (String) propertyParser.resolveProperty(getClass().getSimpleName(), alt);
        }

        URI uri = null;

        try {
            uri = URI.create(src);
        } catch (Exception e) {
            log.warn("Invalid ESI include url: '{}'.", src);
            return;
        }

        HstContainerURL localContainerURL = null;

        if (hstManager != null) {
            try {
                HstRequestContext requestContext = RequestContextProvider.get();
                ResolvedMount curResolvedMount = requestContext.getResolvedMount();
                HstContainerURL curBaseURL = requestContext.getBaseURL();
                String curContextPath = curBaseURL.getContextPath();
                String requestPathFromURL = uri.getPath();

                if (curResolvedMount.getResolvedVirtualHost().getVirtualHost().isContextPathInUrl()) {
                    requestPathFromURL = StringUtils.substringAfter(uri.getPath(), curContextPath);
                }

                localContainerURL = requestContext.getContainerURLProvider().parseURL(curResolvedMount, curContextPath, requestPathFromURL, curBaseURL.getCharacterEncoding());
            } catch (MatchException e) {
                log.debug("The host is not matched by local HST virtual hosts configuration. It might be a remote URL: '{}'.", uri);
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Errors while trying to resolve container URL.", e);
                } else {
                    log.warn("Errors while trying to resolve container URL. {}", e.toString());
                }
            }
        }

        try {
            if (localContainerURL != null) {
                includeLocalURL(writer, uri, localContainerURL);
            } else {
                includeRemoteURL(writer, uri);
            }
        } catch (IOException e) {
            if (StringUtils.isNotEmpty(alt)) {
                if (log.isDebugEnabled()) {
                    log.warn("IOException when processing ESI include element for the source, '" + uri + "'. ALT text, '" + alt + "', is being rendered.", e);
                } else {
                    log.warn("IOException when processing ESI include element for the source, '{}'. ALT text, '{}', is being rendered. " + e, uri, alt);
                }
                writeQuietly(writer, alt);
            } else {
                throw e;
            }
        }
    }

    protected void includeLocalURL(Writer writer, URI uri, HstContainerURL localContainerURL) throws IOException {
        Pipeline pipeline = getESIIncludePipeline();

        if (pipeline == null) {
            log.warn("No pipeline found for ESI includes: '{}'.", esiIncludePipelineName);
            return;
        }

        HstRequestContext requestContext = RequestContextProvider.get();
        HttpServletRequest request = requestContext.getServletRequest();
        HttpServletResponse response = requestContext.getServletResponse();
        HstContainerURL baseURL = requestContext.getBaseURL();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

            GenericResponseWrapper responseWrapper = new GenericResponseWrapper(requestContext.getServletResponse(), baos) {
                private static final long serialVersionUID = 1L;
                @Override
                public boolean isCommitted() {
                    return false;
                }
            };

            ((HstMutableRequestContext) requestContext).setServletResponse(responseWrapper);
            ((HstMutableRequestContext) requestContext).setBaseURL(localContainerURL);

            try {
                pipeline.initialize();
                pipeline.invoke(requestContainerConfig, requestContext, request, responseWrapper);
            } catch (ContainerException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to invoke content window rendering pipeline.", e);
                } else {
                    log.warn("Failed to invoke content window rendering pipeline. {}", e.toString());
                }
            } finally {
                try {
                    pipeline.cleanup(requestContainerConfig, requestContext, request, responseWrapper);
                } catch (ContainerException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to clean up content window rendering pipeline.", e);
                    } else {
                        log.warn("Failed to clean up content window rendering pipeline. {}", e.toString());
                    }
                }
            }

            responseWrapper.flush();

            String charset = responseWrapper.getCharacterEncoding();
            if (StringUtils.isEmpty(charset)) {
                Map<String, String> params = MimeUtil.getHeaderParams(responseWrapper.getContentType());
                charset = StringUtils.defaultIfEmpty(params.get("charset"), "UTF-8");
            }

            String contentBody = baos.toString(charset);
            writer.write(contentBody);
        } finally {
            ((HstMutableRequestContext) requestContext).setServletResponse(response);
            ((HstMutableRequestContext) requestContext).setBaseURL(baseURL);
        }
    }

    protected void includeRemoteURL(Writer writer, URI uri) throws IOException {
        log.warn("Ignoring ESI Include Tag. ESI Include Tag for remote URL is not supported yet: '{}'.", uri);
    }

    protected void writeVarsElementFragment(Writer writer, ESIElementFragment fragment, PropertyParser propertyParser) {
        String source = fragment.getSource();

        if (source != null) {
            source = (String) propertyParser.resolveProperty(getClass().getSimpleName(), source);
            writeQuietly(writer, source);
        }
    }

    protected Pipeline getESIIncludePipeline() {
        Pipeline pipeline = null;

        if (componentManager != null && esiIncludePipelineName != null) {
            Pipelines pipelines = componentManager.getComponent(Pipelines.class.getName());

            if (pipelines != null) {
                pipeline = pipelines.getPipeline(esiIncludePipelineName);
            }
        }

        return pipeline;
    }

    protected PropertyParser createESIPropertyParser(HttpServletRequest request) {
        PropertyParser propertyParser = new PropertyParser(null, "$(", ")", null, true);
        propertyParser.setPlaceholderResolver(new ESIVarsPlaceholderResolver(request));
        return propertyParser;
    }

    private static void writeQuietly(Writer writer, String data) {
        try {
            writer.write(data);
        } catch (IOException e) {
            log.error("Failed to write data.", e);
        }
    }
}
