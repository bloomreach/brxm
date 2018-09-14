/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl.TemplateParameterInfo;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResourceResponseImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.DISPATCH_URI_PROTOCOL;
import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_CLASSPATH_TEMPLATE_PROTOCOL;
import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_JCR_TEMPLATE_PROTOCOL;
import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL;

/**
 * HstComponentInvokerImpl
 *
 * @version $Id$
 */
public class HstComponentInvokerImpl implements HstComponentInvoker {

    private final static Logger log = LoggerFactory.getLogger(HstComponentInvokerImpl.class);

    protected boolean exceptionThrowable;

    protected String errorRenderPath;

    protected String dispatchUrlPrefix;

    public void setExceptionThrowable(boolean exceptionThrowable) {
        this.exceptionThrowable = exceptionThrowable;
    }

    public void setErrorRenderPath(String errorRenderPath) {
        this.errorRenderPath = errorRenderPath;
    }

    public void setDispatchUrlPrefix(String dispatchUrlPrefix) {
        if (dispatchUrlPrefix != null && !dispatchUrlPrefix.startsWith("/")) {
            log.info("The configured dispatchUrlPrefix '{}' does not start with a '/'. We prepend a '/' as the location should be a context relative path.");
            this.dispatchUrlPrefix = "/" + dispatchUrlPrefix;
        } else {
            this.dispatchUrlPrefix = dispatchUrlPrefix;
        }
    }

    public void invokeAction(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();

        if (component != null) {
            try {
                log.debug("invoking action of component: {}", component.getClass().getName());

                component.doAction(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }

                window.addComponentExcpetion(e);
                logComponentException(window, e);
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }

                window.addComponentExcpetion(new HstComponentException(e));
                logComponentException(window, e);
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokePrepareBeforeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();

        if (component != null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("invoking doBeforeRender of component: {}", component.getClass().getName());
                }

                component.prepareBeforeRender(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }

                window.addComponentExcpetion(e);
                logComponentException(window, e);
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }

                window.addComponentExcpetion(new HstComponentException(e));
                logComponentException(window, e);
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeBeforeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();

        if (component != null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("invoking doBeforeRender of component: {}", component.getClass().getName());
                }

                component.doBeforeRender(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }

                window.addComponentExcpetion(e);
                logComponentException(window, e);
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }

                window.addComponentExcpetion(new HstComponentException(e));
                logComponentException(window, e);
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        boolean namedDispatching = false;
        String dispatchUrl = ((HstResponseImpl) hstResponse).getRenderPath();

        if (StringUtils.isBlank(dispatchUrl) && component != null) {

            String templateParameter = null;
            try {
                // we need get the template parameter via the ParameterInfoProxy as the HstParameterInfoProxyFactory
                // can be customized in sub modules, for example for personalization or experiments features
                final TemplateParameterInfo parameterInfoProxy = hstRequest.getRequestContext().getParameterInfoProxyFactory()
                        .createParameterInfoProxy(
                                HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAMETER_INFO_HOLDER.getParametersInfo(),
                                component.getComponentConfiguration(),
                                (HttpServletRequest) hstRequest,
                                (parameterValue, returnType) -> parameterValue);

                templateParameter = parameterInfoProxy.getTemplateParameter();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception while trying to fetch param '{}'", HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME, e);
                } else {
                    log.warn("Exception while trying to fetch param '{}' : {}", HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME, e.toString());
                }
            }

            if (StringUtils.isNotBlank(templateParameter)) {
                dispatchUrl = templateParameter;
            } else {
                dispatchUrl = window.getRenderPath();
            }
        }

        if (dispatchUrl == null) {
            dispatchUrl = window.getNamedRenderer();
            namedDispatching = (dispatchUrl != null);
        }

        ServletRequest wrappedRequest = ((HstRequestImpl) hstRequest).getRequest();

        try {
            setHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse, window);

            if (!StringUtils.isBlank(dispatchUrl)) {
                invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, namedDispatching, dispatchUrl, window);
            } else {
                log.debug("The dispatch url is blank. Component name: '{}'. Component id: '{}'.", window.getName(), window.getComponentInfo().getId());
            }
        } catch (HstComponentException e) {
            if (this.exceptionThrowable) {
                throw e;
            }

            window.addComponentExcpetion(e);
            logComponentException(window, e);
        } catch (Exception e) {
            if (this.exceptionThrowable) {
                throw new HstComponentException(e);
            }

            window.addComponentExcpetion(new HstComponentException(e));

            logComponentException(window, e);
        } finally {
            removeHstObjectAttributesForServlet(wrappedRequest);
        }

        if (window.hasComponentExceptions()) {
            renderErrorInformation(requestContainerConfig, servletRequest, servletResponse, window);
        }
    }

    private void logComponentException(final HstComponentWindow window, final Exception e) {
        if (log.isDebugEnabled()) {
            log.warn("Component exception caught on window {} with component {}", window.getName(),
                    window.getComponentName(), e);
        } else if (log.isWarnEnabled()) {
            // log with the meaningful stack trace element which is the second (index of 1) after this method call most likely.
            log.warn("Component exception caught on window {} with component {}: {} at {}", window.getName(),
                    window.getComponentName(), e.toString(), getIndexedOrFirstStackTraceElement(e, 1));
        }
    }

    public void invokeBeforeServeResource(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();

        if (component != null) {
            try {
                component.doBeforeServeResource(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }

                window.addComponentExcpetion(e);
                logComponentException(window, e);
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }

                window.addComponentExcpetion(new HstComponentException(e));
                logComponentException(window, e);
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeServeResource(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        boolean namedDispatching = false;
        String dispatchUrl = ((HstResourceResponseImpl) hstResponse).getServeResourcePath();

        if (StringUtils.isBlank(dispatchUrl)) {
            dispatchUrl = window.getServeResourcePath();
        }

        if (dispatchUrl == null) {
            dispatchUrl = window.getNamedResourceServer();
            namedDispatching = (dispatchUrl != null);
        }

        // NOTE: No need to dispatch to the render path or named renderer unless a resource template path is explicitly set.
        //       So, stop processing here with info logging in that case. Probably a component's #doBeforeServeResource()
        //       implements everything properly to write (binary) data (e.g, pdf) to the response already without having to
        //       dispatch to any other external servlet or page here.
        if (dispatchUrl == null) {
            log.info("Skipping #invokeServeResource() as the component doesn't have @hst:resourcetemplate "
                    + "property, associated with a servlet or template. Component window: {}, component "
                    + "class: {}.", window.getName(), window.getComponentName());
            return;
        }

        ServletRequest wrappedRequest = ((HstRequestImpl) hstRequest).getRequest();

        try {
            setHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse, window);

            invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, namedDispatching, dispatchUrl, window);

        } catch (HstComponentException e) {
            if (this.exceptionThrowable) {
                throw e;
            }

            window.addComponentExcpetion(e);
            logComponentException(window, e);
        } catch (Exception e) {
            if (this.exceptionThrowable) {
                throw new HstComponentException(e);
            }

            window.addComponentExcpetion(new HstComponentException(e));
            logComponentException(window, e);
        } finally {
            removeHstObjectAttributesForServlet(wrappedRequest);
        }

        if (window.hasComponentExceptions()) {
            renderErrorInformation(requestContainerConfig, servletRequest, servletResponse, window);
        }
    }

    protected void invokeDispatcher(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, boolean namedDispatching, String dispatchUrl, HstComponentWindow window) throws Exception {
        RequestDispatcher disp = null;

        if (!StringUtils.isBlank(dispatchUrl)) {
            log.debug("Invoking dispatcher of url: {}", dispatchUrl);

            if (namedDispatching) {
                disp = requestContainerConfig.getServletContext().getNamedDispatcher(dispatchUrl);
            } else {
                if (dispatchUrl.startsWith(FREEMARKER_JCR_TEMPLATE_PROTOCOL)) {
                    servletRequest.setAttribute(DISPATCH_URI_PROTOCOL, FREEMARKER_JCR_TEMPLATE_PROTOCOL);
                    dispatchUrl = dispatchUrl.substring(FREEMARKER_JCR_TEMPLATE_PROTOCOL.length());
                } else if (dispatchUrl.startsWith(FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL)) {
                    servletRequest.setAttribute(DISPATCH_URI_PROTOCOL, FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL);
                    dispatchUrl = dispatchUrl.substring(FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL.length());
                } else if (dispatchUrl.startsWith(FREEMARKER_CLASSPATH_TEMPLATE_PROTOCOL)) {
                    servletRequest.setAttribute(DISPATCH_URI_PROTOCOL, FREEMARKER_CLASSPATH_TEMPLATE_PROTOCOL);
                    dispatchUrl = dispatchUrl.substring(FREEMARKER_CLASSPATH_TEMPLATE_PROTOCOL.length());

                    if (!dispatchUrl.startsWith("/") && window.getComponent() != null) {
                        String resolvedDispatchUrl = "/" + window.getComponent().getClass().getPackage().getName().replace(".", "/") + "/" + dispatchUrl;

                        log.debug("Relative classpath dispatch URL '{}' has been resolved to '{}'", dispatchUrl, resolvedDispatchUrl);

                        dispatchUrl = resolvedDispatchUrl;
                    }
                } else {
                    if (!dispatchUrl.startsWith("/")) {
                        dispatchUrl = dispatchUrlPrefix + dispatchUrl;
                    }
                }

                disp = requestContainerConfig.getServletContext().getRequestDispatcher(dispatchUrl);
            }
        }

        if (disp == null) {
            log.warn("The request dispatcher for dispatch url '{}' is null. Component name: '{}' . Component class: '" + window.getComponentName() + "'. Component id: '" + window.getComponentInfo().getId() + "'. If the component is inherited, the id might be a concatenation of some id's.", dispatchUrl, window.getName());

            window.addComponentExcpetion(new HstComponentException("The dispatch url is null."));
        } else {
            Task dispatchTask = null;

            try {
                if (HDC.isStarted()) {
                    dispatchTask = HDC.getCurrentTask().startSubtask("Dispatcher");
                    dispatchTask.setAttribute("dispatch", dispatchUrl);
                }

                disp.include(servletRequest, servletResponse);
            } finally {
                if (dispatchTask != null) {
                    dispatchTask.stop();
                }
            }
        }
    }

    protected void renderErrorInformation(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) {
        try {
            servletResponse.reset();

            if (errorRenderPath != null && errorRenderPath.length() != 0) {
                try {
                    servletRequest.setAttribute("errorComponentWindow", window);
                    invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, false, errorRenderPath, window);
                    servletResponse.flushBuffer();
                } finally {
                    servletRequest.removeAttribute("errorComponentWindow");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch to error page: " + e);

            try {
                servletResponse.reset();
            } catch (Exception ignore) {
            }
        }
    }

    private void setHstObjectAttributesForServlet(ServletRequest servletRequest, HstRequest hstRequest,
                                                  HstResponse hstResponse, HstComponentWindow window) {
        // Needs to set hst request/response into attribute map
        // because hst request/response can be wrapped so it's not possible to use casting
        // in the servlet side such as tag library.
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST, hstRequest);
        servletRequest.setAttribute(ContainerConstants.HST_RESPONSE, hstResponse);
        servletRequest.setAttribute(ContainerConstants.HST_COMPONENT_WINDOW, window);
    }

    private void removeHstObjectAttributesForServlet(ServletRequest servletRequest) {
        // Removes hst request/response/componentWindow from attribute map after dispatching
        servletRequest.removeAttribute(ContainerConstants.HST_REQUEST);
        servletRequest.removeAttribute(ContainerConstants.HST_RESPONSE);
        servletRequest.removeAttribute(ContainerConstants.HST_COMPONENT_WINDOW);
    }

    private StackTraceElement getIndexedOrFirstStackTraceElement(final Throwable th, final int index) {
        StackTraceElement[] stackTraceElements = th.getStackTrace();
        final int len = (stackTraceElements != null) ? stackTraceElements.length : 0;

        if (len > index) {
            return stackTraceElements[index];
        } else if (len != 0) {
            return stackTraceElements[0];
        }

        return null;
    }
}
