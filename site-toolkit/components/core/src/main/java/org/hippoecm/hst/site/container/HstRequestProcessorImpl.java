package org.hippoecm.hst.site.container;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstRequestProcessor;
import org.hippoecm.hst.core.container.HttpBufferedResponse;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstRequestProcessorImpl implements HstRequestProcessor {

    public void processAction(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws ContainerException {
        
        HstRequest request = createHstRequest(servletRequest, requestContext, componentConfiguration);
        HstResponse response = createHstResponse(servletResponse, requestContext, componentConfiguration);
        
        HstComponent target = getHstComponent(requestContext, componentConfiguration);
        
        if (target != null) {
            target.doAction(requestContext, request, response);
        }
        
        // After processing action, send a redirect URL for rendering.
        String location = response.getRedirectLocation();

        if (location == null) {
            // TODO: generate location here
        }
        
        redirect(request, response, location);
    }

    public void processBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws ContainerException {
        HstRequest request = createHstRequest(servletRequest, requestContext, componentConfiguration);
        HstResponse response = createHstResponse(servletResponse, requestContext, componentConfiguration);

        HstComponent target = getHstComponent(requestContext, componentConfiguration);
        
        if (target != null) {
            target.doBeforeRender(requestContext, request, response);
        }
    }

    public void processRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws ContainerException {
        HstRequest request = createHstRequest(servletRequest, requestContext, componentConfiguration);
        PrintWriter contentWriter = getComponentContentWriter(requestContext, componentConfiguration);
        HttpBufferedResponse bufferedResponse = new HttpBufferedResponse((HttpServletResponse) servletResponse, contentWriter);
        HstResponse response = createHstResponse(bufferedResponse, requestContext, componentConfiguration);
        
        dispatchComponentRenderPath(requestContext, componentConfiguration, request, response);
    }

    public void processBeforeServeResource(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws ContainerException {
        HstRequest request = createHstRequest(servletRequest, requestContext, componentConfiguration);
        HstResponse response = createHstResponse(servletResponse, requestContext, componentConfiguration);

        HstComponent target = getHstComponent(requestContext, componentConfiguration);
        
        if (target != null) {
            target.doBeforeServeResource(requestContext, request, response);
        }
    }
    
    public void processServeResource(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws ContainerException {
        HstRequest request = createHstRequest(servletRequest, requestContext, componentConfiguration);
        HstResponse response = createHstResponse(servletResponse, requestContext, componentConfiguration);

        dispatchComponentRenderPath(requestContext, componentConfiguration, request, response);
    }

    protected void redirect(HstRequest request, HstResponse response, String location) throws ContainerException
    {
        // Here we intentionally use the original response
        // instead of the wrapped internal response.

        try {
            response.sendRedirect(location);
        } catch (IOException e) {
            throw new ContainerException(e);
        }
    }
    
    protected HstRequest createHstRequest(ServletRequest servletRequest, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) {
        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, componentConfiguration);
        return request;
    }
    
    protected HstResponse createHstResponse(ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) {
        HstResponse response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, componentConfiguration);        
        return response;
    }

    protected HstComponent getHstComponent(HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) {
        HstComponent component = null;
        
        // TODO:
        
        return component;
    }
       
    protected PrintWriter getComponentContentWriter(HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) {
        PrintWriter contentWriter = null;
        
        // TODO:
        
        return contentWriter;
    }
    
    protected void dispatchComponentRenderPath(HstRequestContext requestContext, HstComponentConfiguration componentConfiguration, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        ServletContext componentServletContext = null;
        // TODO: find component's servlet context here.
        RequestDispatcher dispatcher = componentServletContext.getRequestDispatcher(componentConfiguration.getRenderPath());
        
        try {
            dispatcher.include(servletRequest, servletResponse);
        } catch (IOException e) {
            throw new ContainerException(e);
        } catch (ServletException e) {
            throw new ContainerException(e);
        }
    }
}
