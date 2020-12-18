/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.channelmanager.ComponentWindowResponseAppender;
import org.hippoecm.hst.core.component.HeadElement;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_SUPPRESS_WASTE_MESSAGE;

/**
 * AggregationValve
 */
public class AggregationValve extends AbstractBaseOrderableValve {

    /**
     * Flag request attribute name to indicate asynchronous component window rendering in an ancestor window already.
     */
    private static final String ASYNC_RENDERED_BY_ANCESTOR_FLAG_ATTR_NAME = AggregationValve.class.getName() + ".asyncByAncestor";

    private Map<String, AsynchronousComponentWindowRenderer> asynchronousComponentWindowRendererMap;

    private List<ComponentWindowResponseAppender> componentWindowResponseAppenders;

    private boolean asyncComponentRenderingEnabled = true;

    public void setAsynchronousComponentWindowRendererMap(Map<String, AsynchronousComponentWindowRenderer> asynchronousComponentWindowRendererMap) {
        this.asynchronousComponentWindowRendererMap = asynchronousComponentWindowRendererMap;
    }

    public void setComponentWindowResponseAppenders(List<ComponentWindowResponseAppender> componentWindowResponseAppenders) {
        this.componentWindowResponseAppenders = componentWindowResponseAppenders;
    }

    public void setAsyncComponentRenderingEnabled(final boolean asyncComponentRenderingEnabled) {
        this.asyncComponentRenderingEnabled = asyncComponentRenderingEnabled;
    }

    List<ComponentWindowResponseAppender> getComponentWindowResponseAppenders() {
        return componentWindowResponseAppenders;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {

        HstRequestContext requestContext = context.getRequestContext();
        HstContainerURL baseURL = requestContext.getBaseURL();
        String actionWindowReferenceNamespace = baseURL.getActionWindowReferenceNamespace();
        String resourceWindowRef = baseURL.getResourceWindowReferenceNamespace();

        if (actionWindowReferenceNamespace != null || resourceWindowRef != null) {
            // either action or resource request, so skip it
            context.invokeNext();
            return;
        }

        HttpServletRequest servletRequest = requestContext.getServletRequest();
        HttpServletResponse servletResponse = requestContext.getServletResponse();

        if (servletResponse.isCommitted()) {
            log.warn("Stopping aggregated rendering. The response is already committed.");
            context.invokeNext();
            return;
        }

        // If API Documentation request (e.g, /swagger.json), then write a (dynamic) Swagger documentation for example.
        if (isAggregationApiDocumentRequest(context)) {
            writeAggregationApiDocument(context);
            return;
        }

        HstComponentWindow rootWindow = context.getRootComponentWindow();

        HstComponentWindow rootRenderingWindow = context.getRootComponentRenderingWindow();

        if (rootRenderingWindow == null) {
            rootRenderingWindow = rootWindow;
        }

        if (rootWindow == null) {
            log.warn("Skipping aggregated rendering. Cannot find the root component window for '{}'.", servletRequest);
            context.invokeNext();
            return;
        }

        Map<HstComponentWindow, HstRequest> requestMap = new HashMap<HstComponentWindow, HstRequest>();
        Map<HstComponentWindow, HstResponse> responseMap = new HashMap<HstComponentWindow, HstResponse>();

        // make hstRequest and hstResponse for each component window.
        // note that hstResponse is hierarchically created.
        createHstRequestResponseForWindows(rootWindow, rootRenderingWindow, requestContext, servletRequest, servletResponse,
                requestMap, responseMap, null, rootWindow == rootRenderingWindow);

        // to avoid recursive invocation from now, just make a list by hierarchical order.
        List<HstComponentWindow> sortedComponentWindowList = new LinkedList<HstComponentWindow>();

        sortComponentWindowsByHierarchy(rootWindow, sortedComponentWindowList);
        HstComponentWindow[] sortedComponentWindows = sortedComponentWindowList.toArray(new HstComponentWindow[sortedComponentWindowList.size()]);

        // the components that are actually rendered can be a sublist
        HstComponentWindow[] sortedComponentRenderingWindows = sortedComponentWindows;

        if (rootRenderingWindow != rootWindow) {
            // the rendering window is different than the rootWindow. Create a separate ordered list for the 
            // rendering windows
            List<HstComponentWindow> sortedComponentRenderingWindowList = new LinkedList<HstComponentWindow>();
            sortComponentWindowsByHierarchy(rootRenderingWindow, sortedComponentRenderingWindowList);
            sortedComponentRenderingWindows = sortedComponentRenderingWindowList.toArray(new HstComponentWindow[sortedComponentRenderingWindowList.size()]);
        }

        HstContainerConfig requestContainerConfig = context.getRequestContainerConfig();

        // process prepareBeforeRender() of each component
        boolean redirectedOrForwarded = processWindowsPrepareBeforeRender(requestContainerConfig, rootWindow,
                rootRenderingWindow, sortedComponentWindows, requestMap, responseMap);

        if (!redirectedOrForwarded) {
            // process doBeforeRender() of each component as sorted order, parent first.
            processWindowsBeforeRender(requestContainerConfig, rootWindow, rootRenderingWindow, sortedComponentWindows,
                    requestMap, responseMap);
        }

        String redirectLocation = null;

        for (HstComponentWindow window : sortedComponentWindows) {
            if (window.getResponseState().getRedirectLocation() != null) {
                redirectLocation = window.getResponseState().getRedirectLocation();
                break;
            }
        }

        // check if it's requested to forward.
        String forwardPathInfo = rootWindow.getResponseState().getForwardPathInfo();

        // page error handling...
        PageErrors pageErrors = getPageErrors(sortedComponentWindows, true);

        if (pageErrors != null) {
            PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, requestContainerConfig, rootWindow, requestMap.get(rootWindow), responseMap.get(rootWindow));

            // page error handler should be able to override redirect location or forward path info.
            if (rootWindow.getResponseState().getRedirectLocation() != null) {
                redirectLocation = rootWindow.getResponseState().getRedirectLocation();
            }
            // although above the 'forwardPathInfo' is already fetched, it might have been null back then but now
            // rootWindow.getResponseState() might have a forward pathInfo set due to the invocation of handleComponentExceptions
            if (rootWindow.getResponseState().getForwardPathInfo() != null) {
                forwardPathInfo = rootWindow.getResponseState().getForwardPathInfo();
            }

            if (handled == PageErrorHandler.Status.HANDLED_TO_STOP && redirectLocation == null && forwardPathInfo == null) {
                context.invokeNext();
                return;
            }
        }

        // check if any invocation on sendError() exists...
        HstComponentWindow errorCodeSendingWindow = findErrorCodeSendingWindow(sortedComponentWindows);

        if (errorCodeSendingWindow != null) {
            try {
                int errorCode = errorCodeSendingWindow.getResponseState().getErrorCode();
                String errorMessage = errorCodeSendingWindow.getResponseState().getErrorMessage();
                String componentClassName = errorCodeSendingWindow.getComponentName();
                log.debug("The component window has error status code, {} from {}.", errorCode, componentClassName);

                if (errorMessage != null) {
                    servletResponse.sendError(errorCode, errorMessage);
                } else {
                    servletResponse.sendError(errorCode);
                }
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception invocation on sendError().", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception invocation on sendError(). {}", e.toString());
                }
            }
        } else if (redirectLocation != null) {
            try {
                for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
                    HstComponentWindow window = sortedComponentWindows[i];
                    // skip the response body to avoid the response getting already committed (which is not allowed
                    // in case of a redirect
                    window.getResponseState().flush(true);
                }

                boolean permanent = false;
                if (HttpServletResponse.SC_MOVED_PERMANENTLY == rootWindow.getResponseState().getStatus()) {
                    permanent = true;
                }

                if (redirectLocation.startsWith("http:") || redirectLocation.startsWith("https:")) {
                    sendRedirect(servletResponse, redirectLocation, permanent);
                } else if (redirectLocation.startsWith("/")) {
                    if (isAlwaysRedirectLocationToAbsoluteUrl()) {
                    /*
                     * We will redirect to a URL containing the scheme + hostname + portnumber to avoid problems
                     * when redirecting behind a proxy by default.
                     */
                        String fullyQualifiedURL = requestContext.getVirtualHost().getBaseURL(servletRequest) + redirectLocation;
                        sendRedirect(servletResponse, fullyQualifiedURL, permanent);
                    } else {
                        sendRedirect(servletResponse, redirectLocation, permanent);
                    }
                } else {
                    throw new ContainerException("Can only redirect to a context relative path starting with a '/' or " +
                            "to a fully qualified url starting with http: or https: ");
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception during sendRedirect.", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception during sendRedirect. {}", e.toString());
                }
            }
        } else if (forwardPathInfo != null) {

            servletRequest.setAttribute(ContainerConstants.HST_FORWARD_PATH_INFO, forwardPathInfo);

        } else {
            // process HeadContributable's.
            processHeadContributables(context, responseMap.get(rootRenderingWindow));
            // process doRender() of each component as reversed sort order, child first.
            processWindowsRender(requestContainerConfig, sortedComponentRenderingWindows, requestMap, responseMap);
            // handle page errors if exists
            handlePageErrors(context, requestContainerConfig, sortedComponentRenderingWindows, requestMap, responseMap);
            // finally write the aggregated output
            writeAggregatedOutput(context, rootRenderingWindow);
        }

        // continue
        context.invokeNext();
    }

    private static void setNoCacheHeaders(final HttpServletResponse response) {
        response.setDateHeader("Expires", -1);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
    }

    private void sendRedirect(final HttpServletResponse servletResponse, final String redirectLocation, final boolean permanent) throws IOException {
        if (permanent) {
            servletResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            servletResponse.setHeader("Location", redirectLocation);
        } else {
            servletResponse.sendRedirect(redirectLocation);
        }
    }

    protected void createHstRequestResponseForWindows(
            final HstComponentWindow window,
            final HstComponentWindow rootRenderingWindow,
            final HstRequestContext requestContext,
            final ServletRequest servletRequest,
            final ServletResponse parentResponse, // parentResponse of <code>window</code> parameter
            final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap,
            HstResponse topComponentHstResponse,
            boolean isComponentWindowRendered) {

        HstRequest request = new HstRequestImpl((HttpServletRequest)servletRequest, requestContext, window, HstRequest.RENDER_PHASE);
        window.bindResponseState((HttpServletRequest)servletRequest, (HttpServletResponse)parentResponse);

        HstResponse response;
        if (isComponentWindowRendered) {
            response = new HstResponseImpl((HttpServletRequest)servletRequest, (HttpServletResponse)parentResponse,
                    requestContext, window, topComponentHstResponse);
        } else {
            // use a noop response
            response = new NoopHstResponseImpl();
        }

        if (topComponentHstResponse == null && isComponentWindowRendered) {
            topComponentHstResponse = response;
        }

        requestMap.put(window, request);
        responseMap.put(window, response);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();

        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                ServletResponse responseForChild;
                if (isComponentWindowRendered) {
                    responseForChild = response;
                } else {
                    // as long as the componentWindow does not have its renderer invoked, we keep the 
                    // servletResponse for the child component window, until we have the first component window
                    // that is rendered
                    responseForChild = parentResponse;
                }
                boolean isChildComponentWindowRendered = isComponentWindowRendered || entry.getValue() == rootRenderingWindow;
                createHstRequestResponseForWindows(entry.getValue(), rootRenderingWindow, requestContext, servletRequest, responseForChild,
                        requestMap, responseMap, topComponentHstResponse, isChildComponentWindowRendered);
            }
        }
    }

    protected void sortComponentWindowsByHierarchy(
            final HstComponentWindow window,
            final List<HstComponentWindow> sortedWindowList) {

        sortedWindowList.add(window);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();

        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                sortComponentWindowsByHierarchy(entry.getValue(), sortedWindowList);
            }
        }
    }

    protected boolean processWindowsPrepareBeforeRender(
            final HstContainerConfig requestContainerConfig,
            final HstComponentWindow rootWindow,
            final HstComponentWindow rootRenderingWindow,
            final HstComponentWindow[] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap)
            throws ContainerException {
        boolean redirectedOrForwarded = false;
        final boolean asyncCompRenderingEnabled = isAsyncComponentRenderingEnabled();

        for (HstComponentWindow window : sortedComponentWindows) {
            HstRequest request = requestMap.get(window);
            HstResponse response = responseMap.get(window);

            if (window.isVisible() && (!asyncCompRenderingEnabled || !isAsyncComponentWindow(window, request))) {
                getComponentInvoker().invokePrepareBeforeRender(requestContainerConfig, request, response);
            }

            if (window.getResponseState().getRedirectLocation() != null) {
                redirectedOrForwarded = true;
                break;
            }

            if (rootWindow.getResponseState().getForwardPathInfo() != null) {
                redirectedOrForwarded = true;
                break;
            }
        }

        return redirectedOrForwarded;
    }

    protected void processWindowsBeforeRender(
            final HstContainerConfig requestContainerConfig,
            final HstComponentWindow rootWindow,
            final HstComponentWindow rootRenderingWindow,
            final HstComponentWindow[] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap)
            throws ContainerException {

        final boolean asyncCompRenderingEnabled = isAsyncComponentRenderingEnabled();

        for (HstComponentWindow window : sortedComponentWindows) {
            HstRequest request = requestMap.get(window);
            HstResponse response = responseMap.get(window);

            if (window.isVisible()) {
                if (asyncCompRenderingEnabled && isAsyncComponentWindow(window, request)) {
                    if (request.getAttribute(ASYNC_RENDERED_BY_ANCESTOR_FLAG_ATTR_NAME) == Boolean.TRUE) {
                        // we are done with this component because one of its ancestors is loaded async
                        continue;
                    }

                    AsynchronousComponentWindowRenderer asynchronousComponentWindowRenderer = getAsynchronousComponentWindowRenderer(window);

                    if (asynchronousComponentWindowRenderer != null) {
                        asynchronousComponentWindowRenderer.processWindowBeforeRender(window, request, response);
                    } else {
                        log.error("Asynchronous component window rendering skipped! No asynchronousComponentWindowRenderer found for mode, '{}'.", defaultAsynchronousComponentWindowRenderingMode);
                    }
                } else {
                    getComponentInvoker().invokeBeforeRender(requestContainerConfig, request, response);
                }
            }
            if (window.getResponseState().getRedirectLocation() != null) {
                break;
            }

            if (rootWindow.getResponseState().getForwardPathInfo() != null) {
                break;
            }

            if (CollectionUtils.isNotEmpty(componentWindowResponseAppenders)) {
                for (ComponentWindowResponseAppender componentWindowResponseAppender : componentWindowResponseAppenders) {
                    componentWindowResponseAppender.process(rootWindow, rootRenderingWindow, window, request, response);
                }
            }
        }
    }

    /**
     * Process rendering on each HstComponentWindow.
     * <p>
     * Note that after invocation on this method, all the rendering outputs from each HstComponentWindow instance
     * are accumulated into the root HstComponentWindow's {@link HstResponseState}.
     * Therefore, the caller on this method is responsible for flushing the the root HstComponentWindow's {@link HstResponseState}.
     * @param requestContainerConfig {@link HstContainerConfig} instance
     * @param sortedComponentWindows {@link HstComponentWindow} instances array sorted by parent to child order.
     * @param requestMap HstComponentWindow vs HstRequest map
     * @param responseMap HstComponentWindow vs HstResponse map
     * @throws ContainerException if HST Container exception occurs
     */
    protected void processWindowsRender(final HstContainerConfig requestContainerConfig,
                                        final HstComponentWindow[] sortedComponentWindows, final Map<HstComponentWindow, HstRequest> requestMap,
                                        final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {

        final boolean asyncCompRenderingEnabled = isAsyncComponentRenderingEnabled();

        for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
            HstComponentWindow window = sortedComponentWindows[i];
            if (!window.isVisible()) {
                continue;
            }

            HstRequest request = requestMap.get(window);
            if (asyncCompRenderingEnabled && isAsyncComponentWindow(window, request)) {
                continue;
            }
            HstResponse response = responseMap.get(window);
            getComponentInvoker().invokeRender(requestContainerConfig, request, response);

            logPossibleWaste(responseMap, window);
        }
    }

    /**
     * Process {@link HeadContributable} objects in {@link HstRequestContext}.
     * @param context {@link ValveContext} instance
     * @param hstResponse {@link HstResponse} object through which {@link HeadContributable} objects may contribute {@link HeadElement}s.
     * @throws ContainerException if HST Container exception occurs
     */
    private void processHeadContributables(final ValveContext context, final HstResponse hstResponse) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        final Map<String, HeadContributable> headContributableMap = requestContext.getHeadContributableMap();

        if (headContributableMap.isEmpty()) {
            return;
        }

        headContributableMap.values().forEach(headContributable -> headContributable.contributeHeadElements(hstResponse));

    }

    /**
     * Handle page errors by collection all the errors from the sorted {@link HstComponentWindow} array.
     * @param context {@link ValveContext} instance
     * @param requestContainerConfig {@link HstContainerConfig} instance
     * @param sortedComponentWindows {@link HstComponentWindow} instances array sorted by parent to child order.
     * @param requestMap HstComponentWindow vs HstRequest map
     * @param responseMap HstComponentWindow vs HstResponse map
     * @throws ContainerException if HST Container exception occurs
     */
    protected void handlePageErrors(final ValveContext context, final HstContainerConfig requestContainerConfig,
            final HstComponentWindow[] sortedComponentWindows, final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {
        // page error handling...
        final HstComponentWindow rootWindow = context.getRootComponentWindow();
        final PageErrors pageErrors = getPageErrors(sortedComponentWindows, true);

        if (pageErrors != null) {
            PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, requestContainerConfig, rootWindow, requestMap.get(rootWindow), responseMap.get(rootWindow));

            if (handled == PageErrorHandler.Status.HANDLED_TO_STOP) {
                // just ignore because we don't support forward or redirect during rendering...
            }
        }
    }

    /**
     * Write the aggregated output to the client. e.g, by flushing the {@code rootRenderingWindow} by default.
     * @param context {@link ValveContext} instance
     * @param rootRenderingWindow the root rendering {@link HstComponentWindow} instance
     * @throws ContainerException if HST Container exception occurs
     */
    protected void writeAggregatedOutput(final ValveContext context, final HstComponentWindow rootRenderingWindow)
            throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();

        try {
            // add the X-HST-VERSION as a response header if we are in preview:
            boolean isPreviewOrChannelManagerPreviewRequest = requestContext.isPreview() || requestContext.isChannelManagerPreviewRequest();

            if (isPreviewOrChannelManagerPreviewRequest) {
                final HttpServletResponse servletResponse = context.getServletResponse();
                setNoCacheHeaders(servletResponse);

                if (requestContext.getResolvedMount().getMount().isVersionInPreviewHeader()) {
                    servletResponse.setHeader("X-HST-VERSION", HstServices.getImplementationVersion());
                }
            }

            // flush root component window content.
            // note that the child component's contents are already flushed into the root component's response state.
            rootRenderingWindow.getResponseState().flush();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Exception during flushing the response state.", e);
            } else if (log.isWarnEnabled()) {
                log.warn("Exception during flushing the response state. {}", e.toString());
            }
        }
    }

    /**
     * Return true if the current request is for the Aggregation API Document (e.g, /swagger.json). false by default.
     * @param context {@link ValveContext} instance
     * @return true if the current request is for the Aggregation API Document (e.g, /swagger.json). false by default
     */
    protected boolean isAggregationApiDocumentRequest(final ValveContext context) {
        return false;
    }

    /**
     * Do nothing by default. An implementation is supposed to write the Aggregation API Document (e.g, Swagger document)
     * to the client. This method is invoked only if {@link #isAggregationApiDocumentRequest(ValveContext)} returns
     * true.
     * @param context {@link ValveContext} instance
     * @throws ContainerException if HST Container exception occurs
     */
    protected void writeAggregationApiDocument(final ValveContext context)
            throws ContainerException {
    }

    /**
     * Note that HstServletResponseState#flushUnflushedChildrenHeaders(org.hippoecm.hst.core.container.HstComponentWindow) always
     * flushes all components in the end, resulting in finally HstServletResponseState#isFlushed to always return {@code true},
     * *but* at this point in processing, a child component might not yet be flushed because never referenced by an
     * &lt;hst:include&gt; tag.
     */
    private void logPossibleWaste(final Map<HstComponentWindow, HstResponse> responseMap, final HstComponentWindow window) {
        if (!log.isWarnEnabled()) {
            return;
        }
        if (window.getChildWindowMap() == null) {
            return;
        }

        if (!log.isDebugEnabled()) {
            // if log is debug enabled, we keep logging possible waste. If WARN enabled, we log the waste message only
            // ONCE per unique HstComponentInfo instance!
           if (window.getComponentInfo().getAndSetLogWasteMessageProcessed()) {
               return;
           }
        }

        for (HstComponentWindow childWindow : window.getChildWindowMap().values()) {
            final HstResponse childResponse = responseMap.get(childWindow);
            if (!(childResponse instanceof HstResponseImpl)) {
                continue;
            }
            final HstResponseImpl childResponseImpl = (HstResponseImpl)childResponse;
            if (!childWindow.getResponseState().isFlushed() && !childWindow.getComponentInfo().isSuppressWasteMessage()
                    && StringUtils.isNotBlank(getRenderer(childWindow, childResponseImpl))) {
                if (childResponse.getHeadElements() == null || childResponse.getHeadElements().isEmpty()) {
                    // there is a window with a renderer but the content is never included in its ancestor component : That
                    // is a waste. Hence this warning
                    log.warn("POSSIBLE WASTE DETECTED in request '{}' : Component '{}' gets rendered but never adds anything to the response. Its " +
                                    "renderer '{}' is never flushed to a parent component. This might be" +
                                    " waste you are not aware of. If it is on purpose, for example because the component does only some" +
                                    " processing that does not involve direct response contribution, you can mark the component with '{} = true'.",
                            RequestContextProvider.get().getServletRequest(), (childWindow.getComponent() != null) ? childWindow.getComponent().getComponentConfiguration().getCanonicalPath() : null,
                            getRenderer(childWindow, childResponseImpl), COMPONENT_PROPERTY_SUPPRESS_WASTE_MESSAGE);
                } else {
                    // there is a window with a renderer but the content is never included in its ancestor component apart from some
                    // head elements : this is potential waste and unintended behavior has this info
                    log.info("POSSIBLE WASTE DETECTED  in request '{}' : Component '{}' gets rendered but except for some head element(s) adds nothing to the response. " +
                                    "Its renderer '{}' is never flushed to a parent component. This component might be waste." +
                                    " If it is on purpose, for example because the component does only some" +
                                    " processing that does not involve direct response contribution, you can mark the component with '{} = true'.",
                            RequestContextProvider.get().getServletRequest(), (childWindow.getComponent() != null) ? childWindow.getComponent().getComponentConfiguration().getCanonicalPath() : null,
                            getRenderer(childWindow, childResponseImpl), COMPONENT_PROPERTY_SUPPRESS_WASTE_MESSAGE);
                }
            }
        }
    }

    private String getRenderer(final HstComponentWindow window, final HstResponseImpl hstResponse) {
        String dispatchUrl = hstResponse.getRenderPath();
        if (StringUtils.isNotBlank(dispatchUrl)) {
            return dispatchUrl;
        }
        dispatchUrl = window.getRenderPath();
        if (StringUtils.isNotBlank(dispatchUrl)) {
            return dispatchUrl;
        }
        return window.getNamedRenderer();
    }

    /**
     * returns {@code true} if the asynchronous component window rendering is enabled. {@code true} by default.
     * <P>
     * <EM>Note</EM>: By default, it returns {@code false} if the request comes from the Channel Manager preview UI,
     * if it is a Component Rendering URL request, or if the request comes from a Search Engine.
     * Otherwise, returns {@code true} by default.
     * @return {@code true} if the asynchronous component window rendering is enabled
     */
    protected boolean isAsyncComponentRenderingEnabled() {
        final HstRequestContext requestContext = RequestContextProvider.get();

        // in cms request context, we never load asynchronous
        if (requestContext.isChannelManagerPreviewRequest()) {
            return false;
        }

        if (requestContext.getBaseURL().getComponentRenderingWindowReferenceNamespace() != null) {
            return false;
        }

        if (requestContext.isSearchEngineOrBotRequest()) {
            return false;
        }

        return asyncComponentRenderingEnabled;
    }

    /**
     * returns <code>true</code> when the component window is marked as async. When the component is async due to a
     * ancestor is async, we also set <code>{@link #ASYNC_RENDERED_BY_ANCESTOR_FLAG_ATTR_NAME}</code> is
     * <code>true</code> on the HstRequest to indicate async due to ancestor
     *
     * @param window
     * @param request
     * @return
     */
    private boolean isAsyncComponentWindow(final HstComponentWindow window, final HstRequest request) {
        // check if there is already an async parent.
        HstComponentWindow parent = window.getParentWindow();
        while (parent != null) {
            if (parent.getComponentInfo().isAsync()) {
                request.setAttribute(ASYNC_RENDERED_BY_ANCESTOR_FLAG_ATTR_NAME, Boolean.TRUE);
                return true;
            }
            parent = parent.getParentWindow();
        }

        // check whether the component itself is asyn
        return window.getComponentInfo().isAsync();

    }

    private AsynchronousComponentWindowRenderer getAsynchronousComponentWindowRenderer(final HstComponentWindow window) {
        AsynchronousComponentWindowRenderer asynchronousComponentWindowRenderer = null;

        if (asynchronousComponentWindowRendererMap != null) {
            String asyncMode = window.getComponentInfo().getAsyncMode();

            if (StringUtils.isNotEmpty(asyncMode)) {
                asynchronousComponentWindowRenderer = asynchronousComponentWindowRendererMap.get(asyncMode);

                if (asynchronousComponentWindowRenderer == null) {
                    log.warn("Unsupported asyncMode '{}' found for '{}'. Using default asyncMode '{}' instead. " +
                                    "Supported asyncModes are '{}'.",
                            new String[]{asyncMode, window.getComponentInfo().getId(), defaultAsynchronousComponentWindowRenderingMode, asynchronousComponentWindowRendererMap.keySet().toString()});
                }
            }

            if (asynchronousComponentWindowRenderer == null) {
                asynchronousComponentWindowRenderer = asynchronousComponentWindowRendererMap.get(defaultAsynchronousComponentWindowRenderingMode);
            }
        }

        return asynchronousComponentWindowRenderer;
    }

    private class NoopHstResponseImpl implements HstResponse {
        /**
         * the {@link NoopHstResponseImpl} always gets its renderer skipped
         */
        @Override
        public boolean isRendererSkipped() {
            return true;
        }

        @Override
        public void addCookie(Cookie cookie) {
        }

        @Override
        public void addHeadElement(Element element, String keyHint) {
        }

        @Override
        public void addPreamble(Comment comment) {
        }

        @Override
        public void addPreamble(Element element) {
        }

        @Override
        public List<Node> getPreambleNodes() {
            return null;
        }

        @Override
        public void addEpilogue(Comment comment) {
        }

        @Override
        public List<Node> getEpilogueNodes() {
            return null;
        }

        @Override
        public boolean containsHeadElement(String keyHint) {
            return false;
        }

        @Override
        public void addProcessedHeadElement(final Element headElement) {
        }

        @Override
        public HstURL createActionURL() {
            return null;
        }

        @Override
        public Comment createComment(String comment) {
            return null;
        }

        @Override
        public HstURL createComponentRenderingURL() {
            return null;
        }

        @Override
        public Element createElement(String tagName) {
            return null;
        }

        @Override
        public HstURL createNavigationalURL(String pathInfo) {
            return null;
        }

        @Override
        public HstURL createRenderURL() {
            return null;
        }

        @Override
        public HstURL createResourceURL() {
            return null;
        }

        @Override
        public HstURL createResourceURL(String referenceNamespace) {
            return null;
        }

        @Override
        public void flushChildContent(String name) throws IOException {
        }

        @Override
        public void flushChildContent(final String name, final Writer writer) throws IOException {
        }

        @Override
        public void forward(String pathInfo) throws IOException {
        }

        @Override
        public List<String> getChildContentNames() {
            return null;
        }

        @Override
        public List<Element> getHeadElements() {
            return null;
        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public Element getWrapperElement() {
            return null;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
        }

        @Override
        public void sendError(int sc) throws IOException {
        }

        @Override
        public void sendRedirect(String location) throws IOException {
        }

        @Override
        public void setRenderParameter(String key, String value) {
        }

        @Override
        public void setRenderParameter(String key, String[] values) {
        }

        @Override
        public void setRenderParameters(Map<String, String[]> parameters) {
        }

        @Override
        public void setRenderPath(String renderPath) {
        }

        @Override
        public void setServeResourcePath(String serveResourcePath) {
        }

        @Override
        public void setStatus(int sc) {
        }

        @Override
        public void setWrapperElement(Element element) {
        }

        @Override
        public void addDateHeader(String name, long date) {
        }

        @Override
        public void addHeader(String name, String value) {
        }

        @Override
        public void addIntHeader(String name, int value) {
        }

        @Override
        public boolean containsHeader(String name) {
            return false;
        }

        @Override
        public String encodeRedirectURL(String url) {
            return null;
        }

        @Override
        public String encodeRedirectUrl(String url) {
            return null;
        }

        @Override
        public String encodeURL(String url) {
            return null;
        }

        @Override
        public String encodeUrl(String url) {
            return null;
        }

        @Override
        public void setDateHeader(String name, long date) {
        }

        @Override
        public void setHeader(String name, String value) {
        }

        @Override
        public void setIntHeader(String name, int value) {
        }

        @Override
        public void setStatus(int sc, String sm) {
        }

        @Override
        public void flushBuffer() throws IOException {
        }

        @Override
        public int getBufferSize() {
            return 0;
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return null;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return null;
        }

        @Override
        public boolean isCommitted() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public void resetBuffer() {
        }

        @Override
        public void setBufferSize(int size) {
        }

        @Override
        public void setCharacterEncoding(String charset) {
        }

        @Override
        public void setContentLength(int len) {
        }

        @Override
        public void setContentLengthLong(long len) {
            
        }

        @Override
        public void setContentType(String type) {
        }

        @Override
        public void setLocale(Locale loc) {
        }

        /*
         * Servlet Spec 3.0 APIs
         */
        public int getStatus() {
            return HttpServletResponse.SC_OK;
        }

        public String getHeader(String name) {
            return null;
        }

        public Collection<String> getHeaders(String name) {
            return Collections.emptyList();
        }

        public Collection<String> getHeaderNames() {
            return Collections.emptyList();
        }
    }

}
