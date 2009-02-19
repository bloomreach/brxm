package org.hippoecm.hst.core.container;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.component.HstURL;

public class HstContainerURLImpl implements HstContainerURL {

    protected String contextPath;
    protected String servletPath;
    protected String renderPath;
    protected String type = HstURL.TYPE_RENDER;
    protected String actionWindow;
    protected String resourceWindow;
    protected Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    public String getContextPath() {
        return this.contextPath;
    }
    
    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }
    
    public String getServletPath() {
        return this.servletPath;
    }
    
    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }
    
    public String getRenderPath() {
        return this.renderPath;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return this.type;
    }
    
    public void setActionWindow(String actionWindow) {
        this.actionWindow = actionWindow;
    }
    
    public String getActionWindow() {
        return this.actionWindow;
    }
    
    public void setResourceWindow(String resourceWindow) {
        this.resourceWindow = resourceWindow;
    }
    
    public String getResourceWindow() {
        return this.resourceWindow;
    }
    
    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    public void setParameter(String name, String value) {
        this.parameterMap.put(name, new String [] { value });
    }

    public void setParameter(String name, String[] values) {
        this.parameterMap.put(name, values);
    }

    public void setParameters(Map<String, String[]> parameters) {
        this.parameterMap.putAll(parameters);
    }

}
