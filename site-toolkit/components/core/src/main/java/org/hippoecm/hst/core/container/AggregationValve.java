/*
 *  Copyright 2008 Hippo.
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstServletResponseState;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * AggregationValve
 * 
 * @version $Id$
 */
public class AggregationValve extends AbstractValve {

    @Override
    public void invoke(ValveContext context) throws ContainerException {

        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();
        
        if (!context.getServletResponse().isCommitted() && requestContext.getBaseURL().getResourceWindowReferenceNamespace() == null) {
            HstComponentWindow rootWindow = context.getRootComponentWindow();
            HstComponentWindow rootRenderingWindow = context.getRootComponentRenderingWindow();
            if(rootRenderingWindow == null) {
                rootRenderingWindow = rootWindow;
            }
            if (rootWindow != null) {
                
                Map<HstComponentWindow, HstRequest> requestMap = new HashMap<HstComponentWindow, HstRequest>();
                Map<HstComponentWindow, HstResponse> responseMap = new HashMap<HstComponentWindow, HstResponse>();

                ServletRequest parentRequest = servletRequest;
                ServletResponse parentResponse = servletResponse;
                
                HstResponse topParentResponse = null;
                
                // Check if it is invoked from portlet.
                HstResponseState portletHstResponseState = (HstResponseState) servletRequest.getAttribute(HstResponseState.class.getName());
                
                if (portletHstResponseState != null) {
                    parentResponse = new HstResponseImpl((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, requestContext, null, portletHstResponseState, null);
                    topParentResponse = (HstResponse) parentResponse;
                }
                
                // make hstRequest and hstResponse for each component window.
                // note that hstResponse is hierarchically created.
                createHstRequestResponseForWindows(rootWindow, rootRenderingWindow, requestContext, parentRequest, parentResponse,
                        requestMap, responseMap, topParentResponse, rootWindow == rootRenderingWindow );
                
                // to avoid recursive invocation from now, just make a list by hierarchical order.
                List<HstComponentWindow> sortedComponentWindowList = new LinkedList<HstComponentWindow>();

                sortComponentWindowsByHierarchy(rootWindow, sortedComponentWindowList);
                HstComponentWindow [] sortedComponentWindows = sortedComponentWindowList.toArray(new HstComponentWindow[sortedComponentWindowList.size()]);
                
                // the components that are actually rendered can be a sublist
                HstComponentWindow [] sortedComponentRenderingWindows = sortedComponentWindows;
                if(rootRenderingWindow != rootWindow) {
                    // the rendering window is different than the rootWindow. Create a separate ordered list for the 
                    // rendering windows
                    List<HstComponentWindow> sortedComponentRenderingWindowList = new LinkedList<HstComponentWindow>();
                    sortComponentWindowsByHierarchy(rootRenderingWindow, sortedComponentRenderingWindowList);
                    sortedComponentRenderingWindows = sortedComponentRenderingWindowList.toArray(new HstComponentWindow[sortedComponentRenderingWindowList.size()]);
                } 
                
                
                HstContainerConfig requestContainerConfig = context.getRequestContainerConfig();
                // process doBeforeRender() of each component as sorted order, parent first.
                processWindowsBeforeRender(requestContainerConfig, rootWindow, rootRenderingWindow, sortedComponentWindows, requestMap, responseMap);
                
                String redirectLocation = null;
                if (!requestContext.isPortletContext()) {
                    for (HstComponentWindow window : sortedComponentWindows) {
                        if (window.getResponseState().getRedirectLocation() != null) {
                            redirectLocation = window.getResponseState().getRedirectLocation();
                            break;
                        }
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
                            window.getResponseState().flush();
                        }
                        
                        if (redirectLocation.startsWith("http:") || redirectLocation.startsWith("https:")) {
                            servletResponse.sendRedirect(redirectLocation);
                        } else {
                            if (!redirectLocation.startsWith("/")) {
                                throw new ContainerException("Can only redirect to a context relative path starting with a '/'.");
                            }
                            
                            /* 
                             * We will redirect to a URL containing the scheme + hostname + portnumber to avoid problems
                             * when redirecting behind a proxy by default.
                             */
                            if (isAlwaysRedirectLocationToAbsoluteUrl()) {
                                String absoluteRedirectUrl = requestContext.getVirtualHost().getBaseURL(servletRequest) + redirectLocation;
                                servletResponse.sendRedirect(absoluteRedirectUrl);
                            } else {
                                servletResponse.sendRedirect(redirectLocation);
                            }
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
                     
                    // process doRender() of each component as reversed sort order, child first.
                    processWindowsRender(requestContainerConfig, sortedComponentRenderingWindows, requestMap, responseMap);
                    
                    // page error handling...
                    pageErrors = getPageErrors(sortedComponentWindows, true);
                    if (pageErrors != null) {
                        PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, requestContainerConfig, rootWindow, requestMap.get(rootWindow), responseMap.get(rootWindow));
                        if (handled == PageErrorHandler.Status.HANDLED_TO_STOP) {
                            // just ignore because we don't support forward or redirect during rendering...
                        }
                    }
                    
                    try {
                        // add the X-HST-VERSION as a response header if we are in preview:
                        if (rootWindow == rootRenderingWindow && requestContext.isPreview() && requestContext.getResolvedMount().getMount().isVersionInPreviewHeader()) {
                            rootWindow.getResponseState().addHeader("X-HST-VERSION", HstServices.getImplementationVersion());
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
            }
        }
        
        // continue
        context.invokeNext();
    }

    protected void createHstRequestResponseForWindows(
            final HstComponentWindow window,
            final HstComponentWindow rootRenderingWindow, 
            final HstRequestContext requestContext, 
            final ServletRequest servletRequest,
            final ServletResponse servletResponse, 
            final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap,
            HstResponse topComponentHstResponse,
            boolean isComponentWindowRendered) {

        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window, HstRequest.RENDER_PHASE);
        HstResponse response = null;
        HstResponseState responseState = null;
        if(isComponentWindowRendered) {
            responseState = new HstServletResponseState((HttpServletRequest) servletRequest,
                    (HttpServletResponse) servletResponse);
                response= new HstResponseImpl((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, requestContext, window,
                    responseState, topComponentHstResponse);
        } else {
            // use a noop responseState and noop response
            responseState = new NoopHstServletResponseState();
            response = new NoopHstResponseImpl();
        }
        
        if (topComponentHstResponse == null && isComponentWindowRendered) {
            topComponentHstResponse = response;
        }

        requestMap.put(window, request);
        responseMap.put(window, response);

        ((HstComponentWindowImpl) window).setResponseState(responseState);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();

        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                ServletResponse responseForChild;
                if(isComponentWindowRendered) {
                    responseForChild = response;
                } else {
                    // as long as the componentWindow does not have its renderer invoked, we keep the 
                    // servletResponse for the child component window, until we have the first component window
                    // that is rendered
                    responseForChild = servletResponse;
                }
                boolean isChildComponentWindowRendered = isComponentWindowRendered || entry.getValue() == rootRenderingWindow;
                createHstRequestResponseForWindows(entry.getValue(), rootRenderingWindow, requestContext, servletRequest, responseForChild,
                        requestMap, responseMap, topComponentHstResponse, isChildComponentWindowRendered );
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
    
    protected void processWindowsBeforeRender(
            final HstContainerConfig requestContainerConfig, 
            final HstComponentWindow rootWindow,
            final HstComponentWindow rootRenderingWindow,
            final HstComponentWindow [] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponse> responseMap)
            throws ContainerException {

        for (int i = 0; i < sortedComponentWindows.length; i++) {
            HstComponentWindow window = sortedComponentWindows[i];
            HstRequest request = requestMap.get(window);
            HstResponse response = responseMap.get(window);

            if (window.isVisible()) {
                // in cms request context, we never load asynchronous
                if (isAsync(window, request)) {
                    if (request.getAttribute(AggregationValve.class.getName() + ".asyncByAncestor") == Boolean.TRUE) {
                        // we are done with this component because one of its ancestors is loaded async
                        continue;
                    }
                    HstURLFactory urlFactory = request.getRequestContext().getURLFactory();
                    HstURL url = urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE,
                            window.getReferenceNamespace(), null,
                            request.getRequestContext());
                    Element hiddenDiv = response.createElement("div");
                    hiddenDiv.setAttribute("id", url.toString());
                    hiddenDiv.setAttribute("class", "_async");
                    hiddenDiv.setAttribute("style", "display:none;");
                    response.addPreamble(hiddenDiv);

                    if (!response.containsHeadElement(AggregationValve.class.getName() + ".async")) {
                        Element headScript = response.createElement("script");
                        String src = request.getRequestContext().getHstLinkCreator().create("resources/simple-io.js", request.getRequestContext().getResolvedMount().getMount(), true).toUrlForm(request.getRequestContext(), false);
                        headScript.setAttribute("src",src);
                        headScript.setAttribute("type","text/javascript");
                        response.addHeadElement(headScript, AggregationValve.class.getName() + ".async");

                        Element endBodyScript = response.createElement("script");
                        endBodyScript.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, "scripts");
                        endBodyScript.setAttribute("type","text/javascript");
                        endBodyScript.setTextContent("Hippo.Hst.AsyncPage.load();");
                        response.addHeadElement(endBodyScript, "asyncLoad");

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
            
            HttpSession session = request.getSession(false);
            
            if(session != null && request.getRequestContext().isCmsRequest()) {
                Boolean composerMode = (Boolean) session.getAttribute(ContainerConstants.COMPOSER_MODE_ATTR_NAME);
                if (composerMode != null) {
                    Mount mount = request.getRequestContext().getResolvedMount().getMount();
                    // we are in render host mode. Add the wrapper elements that are needed for the composer around all components
                    HstComponentConfiguration compConfig  = ((HstComponentConfiguration)window.getComponentInfo());
                    if (rootWindow == rootRenderingWindow && window == rootWindow) {
                        rootWindow.getResponseState().addHeader("HST-Mount-Id", mount.getIdentifier());
                        rootWindow.getResponseState().addHeader("HST-Site-Id", mount.getHstSite().getCanonicalIdentifier());
                        rootWindow.getResponseState().addHeader("HST-Page-Id", compConfig.getCanonicalIdentifier());
                        if (mount instanceof MutableMount) {
                            MutableMount mutableMount = (MutableMount)mount;
                            final String lockedBy = mutableMount.getLockedBy();
                            if (StringUtils.isNotBlank(lockedBy)) {
                                rootWindow.getResponseState().addHeader("HST-Mount-LockedBy", lockedBy);
                                rootWindow.getResponseState().addHeader("HST-Mount-LockedOn", String.valueOf(mutableMount.getLockedOn().getTimeInMillis()));
                            }
                        }
                        Object variant = session.getAttribute(ContainerConstants.RENDER_VARIANT);
                        if (variant == null) {
                            variant = "default";
                        }
                        rootWindow.getResponseState().addHeader("HST-Render-Variant", variant.toString());
                        boolean isPreviewConfig = false;
                        if(mount.getHstSite().getConfigurationPath().endsWith("-"+Mount.PREVIEW_NAME)) {
                            isPreviewConfig = true;
                        }
                        rootWindow.getResponseState().addHeader("HST-Site-HasPreviewConfig", String.valueOf(isPreviewConfig));
                        //"-" + Mount.PREVIEW_NAME;
                    } else if(Boolean.TRUE.equals(composerMode)) {
                        HashMap<String, String> attributes = new HashMap<String, String>();
                        attributes.put("uuid", compConfig.getCanonicalIdentifier());
                        if(compConfig.getXType() != null) {
                            attributes.put("xtype", compConfig.getXType());
                        }
                        if(compConfig.isInherited()) {
                            attributes.put("inherited", "true"); 
                        }
                        attributes.put("type", compConfig.getComponentType().toString());
                        HstURLFactory urlFactory = request.getRequestContext().getURLFactory();
                        HstURL url = urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE,
                                                          window.getReferenceNamespace(), null,
                                                          request.getRequestContext());
                        attributes.put("url", url.toString());
                        attributes.put("refNS", window.getReferenceNamespace());

                        Comment comment = createCommentWithAttr(attributes, response);
                        response.addPreamble(comment);
                    }
                } 
            }
        }
    }

    /**
     * returns <code>true</code> when the component window is marked as async. When the component is async
     * due to a ancestor is async, we also set XXX is <code>true</code> on the HstRequest to indicate async due to ancestor
     * @param window
     * @param request
     * @return
     */
    private boolean isAsync(final HstComponentWindow window, final HstRequest request) {
        if (request.getRequestContext().isCmsRequest()) {
            return false;
        } 
        if (request.getRequestContext().getBaseURL().getComponentRenderingWindowReferenceNamespace() != null) {
            return false;
        }
        if (window.getComponentInfo().isAsync()) {
            return true;
        }
        HstComponentWindow parent = window.getParentWindow();
        while (parent != null) {
            if (parent.getComponentInfo().isAsync()) {
                request.setAttribute(AggregationValve.class.getName() + ".asyncByAncestor", Boolean.TRUE);
                return true;
            }
            parent = parent.getParentWindow();
        }
        return false;
    }

    protected void processWindowsRender(final HstContainerConfig requestContainerConfig,
            final HstComponentWindow[] sortedComponentWindows, final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {

        for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
            HstComponentWindow window = sortedComponentWindows[i];
            if (!window.isVisible()) {
                continue;
            }

            HstRequest request = requestMap.get(window);
            if (window.getComponentInfo().isAsync() && request.getRequestContext().getBaseURL().getComponentRenderingWindowReferenceNamespace() == null) {
                continue;
            }
            HstResponse response = responseMap.get(window);
            getComponentInvoker().invokeRender(requestContainerConfig, request, response);
        }
    }

    // TODO replace by json marshaller
    private Comment createCommentWithAttr(HashMap<String, String> attributes, HstResponse response) {
        StringBuilder builder = new StringBuilder();
        for(Entry<String, String> attr : attributes.entrySet()) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append("\"").append(attr.getKey()).append("\":").append("\"").append(attr.getValue()).append("\"");
        }
        Comment comment = response.createComment("{ " + builder.toString() +"}");
        return comment;
    }

    private class NoopHstServletResponseState implements HstResponseState {
        @Override public void addCookie(Cookie cookie) {}
        @Override public void addDateHeader(String name, long date) {}
        @Override public void addHeadElement(Element element, String keyHint) {}
        @Override public void addHeader(String name, String value) {}
        @Override public void addIntHeader(String name, int value) {}
        @Override public void addPreambleNode(Comment comment) {}
        @Override public void addPreambleNode(Element element) {}
        @Override public void clear() {}
        @Override public boolean containsHeadElement(String keyHint) {return false;}
        @Override public boolean containsHeader(String name) {return false;}
        @Override public Comment createComment(String comment) {return null;}
        @Override public Element createElement(String tagName) {return null;}
        @Override public void flush() throws IOException {}
        @Override public void flushBuffer() throws IOException {}
        @Override public void forward(String pathInfo) throws IOException {}
        @Override public int getBufferSize() {return 0;}
        @Override public String getCharacterEncoding() {return null;}
        @Override public String getContentType() {return null;}
        @Override public int getErrorCode() {return 0;}
        @Override public String getErrorMessage() {return null;}
        @Override public String getForwardPathInfo() {return null;}
        @Override public List<Element> getHeadElements() {return null;}
        @Override public Locale getLocale() {return null;}
        @Override public ServletOutputStream getOutputStream() throws IOException {return null;}
        @Override public String getRedirectLocation() {return null;}
        @Override public Element getWrapperElement() {return null;}
        @Override public PrintWriter getWriter() throws IOException {return null;}
        @Override public boolean isActionResponse() {return false;}
        @Override public boolean isCommitted() {return false;}
        @Override public boolean isMimeResponse() {return false;}
        @Override public boolean isRenderResponse() {return false;}
        @Override public boolean isResourceResponse() {return false;}
        @Override public boolean isStateAwareResponse() {return false;}
        @Override public void reset() {}
        @Override public void resetBuffer() {}
        @Override public void sendError(int errorCode, String errorMessage) throws IOException {}
        @Override public void sendError(int errorCode) throws IOException {}
        @Override public void sendRedirect(String redirectLocation) throws IOException {}
        @Override public void setBufferSize(int size) {}
        @Override public void setCharacterEncoding(String charset) {}
        @Override public void setContentLength(int len) {}
        @Override public void setContentType(String type) {}
        @Override public void setDateHeader(String name, long date) {}
        @Override public void setHeader(String name, String value) {}
        @Override public void setIntHeader(String name, int value) {}
        @Override public void setLocale(Locale locale) {}
        @Override public void setStatus(int statusCode, String message) {}
        @Override public void setStatus(int statusCode) {}
        @Override public void setWrapperElement(Element element) {}
        
    }
    
    private class NoopHstResponseImpl implements HstResponse {  /**
         * the {@link NoopHstResponseImpl} always gets its renderer skipped
         */
        @Override public boolean isRendererSkipped() {            
            return true;
        }
        @Override public void addCookie(Cookie cookie) {}
        @Override public void addHeadElement(Element element, String keyHint) {}
        @Override public void addPreamble(Comment comment) {}
        @Override public void addPreamble(Element element) {}
        @Override public boolean containsHeadElement(String keyHint) {return false;}
        @Override public HstURL createActionURL() {return null;}
        @Override public Comment createComment(String comment) {return null;}
        @Override public HstURL createComponentRenderingURL() {return null;}
        @Override public Element createElement(String tagName) {return null;}
        @Override public HstURL createNavigationalURL(String pathInfo) {return null;}
        @Override public HstURL createRenderURL() {return null;}
        @Override public HstURL createResourceURL() {return null;}
        @Override public HstURL createResourceURL(String referenceNamespace) {return null;}
        @Override public void flushChildContent(String name) throws IOException {}
        @Override public void forward(String pathInfo) throws IOException {}
        @Override public List<String> getChildContentNames() {return null;}
        @Override public List<Element> getHeadElements() {return null;}
        @Override public String getNamespace() {return null;}
        @Override public Element getWrapperElement() {return null;}
        @Override public void sendError(int sc, String msg) throws IOException {}
        @Override public void sendError(int sc) throws IOException {}
        @Override public void sendRedirect(String location) throws IOException {}
        @Override public void setRenderParameter(String key, String value) {}
        @Override public void setRenderParameter(String key, String[] values) {}
        @Override public void setRenderParameters(Map<String, String[]> parameters) {}
        @Override public void setRenderPath(String renderPath) {}
        @Override public void setServeResourcePath(String serveResourcePath) {}
        @Override public void setStatus(int sc) {}
        @Override public void setWrapperElement(Element element) {}
        @Override public void addDateHeader(String name, long date) {}
        @Override public void addHeader(String name, String value) {}
        @Override public void addIntHeader(String name, int value) {}
        @Override public boolean containsHeader(String name) {return false;}
        @Override public String encodeRedirectURL(String url) {return null;}
        @Override public String encodeRedirectUrl(String url) {return null;}
        @Override public String encodeURL(String url) {return null;}
        @Override public String encodeUrl(String url) {return null;}
        @Override public void setDateHeader(String name, long date) {}
        @Override public void setHeader(String name, String value) {}
        @Override public void setIntHeader(String name, int value) {}
        @Override public void setStatus(int sc, String sm) {}
        @Override public void flushBuffer() throws IOException {}
        @Override public int getBufferSize() {return 0;}
        @Override public String getCharacterEncoding() {return null;}
        @Override public String getContentType() {return null;}
        @Override public Locale getLocale() {return null;}
        @Override public ServletOutputStream getOutputStream() throws IOException {return null;}
        @Override public PrintWriter getWriter() throws IOException {return null;}
        @Override public boolean isCommitted() {return false;}
        @Override public void reset() {}
        @Override public void resetBuffer() {}
        @Override public void setBufferSize(int size) {}
        @Override public void setCharacterEncoding(String charset) {}
        @Override public void setContentLength(int len) {}
        @Override public void setContentType(String type) {}
        @Override public void setLocale(Locale loc) {}
    }


}
