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

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentWindow;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstRequestProcessor;
import org.hippoecm.hst.core.container.HttpBufferedResponse;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstRequestProcessorImpl implements HstRequestProcessor {

    public void processAction(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException {
        
        HstRequest request = createHstRequest(servletRequest, requestContext, componentWindow);
        HstResponse response = createHstResponse(servletResponse, requestContext, componentWindow);
        
        HstComponent target = getHstComponent(requestContext, componentWindow);
        
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

    public void processBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException {
        HstRequest request = createHstRequest(servletRequest, requestContext, componentWindow);
        HstResponse response = createHstResponse(servletResponse, requestContext, componentWindow);

        HstComponent target = getHstComponent(requestContext, componentWindow);
        
        if (target != null) {
            target.doBeforeRender(requestContext, request, response);
        }
    }

    public void processRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException {
        HstRequest request = createHstRequest(servletRequest, requestContext, componentWindow);
        PrintWriter contentWriter = getComponentContentWriter(requestContext, componentWindow);
        HttpBufferedResponse bufferedResponse = new HttpBufferedResponse((HttpServletResponse) servletResponse, contentWriter);
        HstResponse response = createHstResponse(bufferedResponse, requestContext, componentWindow);
        
        dispatchComponentRenderPath(requestContext, componentWindow, request, response);
    }

    public void processBeforeServeResource(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException {
        HstRequest request = createHstRequest(servletRequest, requestContext, componentWindow);
        HstResponse response = createHstResponse(servletResponse, requestContext, componentWindow);

        HstComponent target = getHstComponent(requestContext, componentWindow);
        
        if (target != null) {
            target.doBeforeServeResource(requestContext, request, response);
        }
    }
    
    public void processServeResource(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException {
        HstRequest request = createHstRequest(servletRequest, requestContext, componentWindow);
        HstResponse response = createHstResponse(servletResponse, requestContext, componentWindow);

        dispatchComponentRenderPath(requestContext, componentWindow, request, response);
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
    
    protected HstRequest createHstRequest(ServletRequest servletRequest, HstRequestContext requestContext, HstComponentWindow componentWindow) {
        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, componentWindow);
        return request;
    }
    
    protected HstResponse createHstResponse(ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) {
        HstResponse response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, componentWindow);        
        return response;
    }

    protected HstComponent getHstComponent(HstRequestContext requestContext, HstComponentWindow componentWindow) {
        HstComponent component = null;
        
        // TODO:
        
        return component;
    }
       
    protected PrintWriter getComponentContentWriter(HstRequestContext requestContext, HstComponentWindow componentWindow) {
        PrintWriter contentWriter = null;
        
        // TODO:
        
        return contentWriter;
    }
    
    protected void dispatchComponentRenderPath(HstRequestContext requestContext, HstComponentWindow componentWindow, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        ServletContext componentServletContext = null;
        // TODO: find component's servlet context here.
        RequestDispatcher dispatcher = componentServletContext.getRequestDispatcher(componentWindow.getRenderPath());
        
        try {
            dispatcher.include(servletRequest, servletResponse);
        } catch (IOException e) {
            throw new ContainerException(e);
        } catch (ServletException e) {
            throw new ContainerException(e);
        }
    }
}
