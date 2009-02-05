package org.hippoecm.hst.core.component;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public class HstRequestImpl extends HttpServletRequestWrapper implements HstRequest {
    
    protected HstComponentConfiguration componentConfig;

    public HstRequestImpl(HttpServletRequest servletRequest, HstComponentConfiguration componentConfig) {
        super(servletRequest);
        this.componentConfig = componentConfig;
    }

    //  ServletRequestWrapper overlay

    public String getParameter(String name) {
        String encodedName = this.componentConfig.getNamespace() + name;
        Object value = this.getParameterMap().get(encodedName);

        if (value == null) {
            return (null);
        } else if (value instanceof String[]) {
            return (((String[]) value)[0]);
        } else if (value instanceof String) {
            return ((String) value);
        } else {
            return (value.toString());
        }
    }

    public Map getParameterMap() {
        return null;
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(this.getParameterMap().keySet());
    }

    public String[] getParameterValues(String name) {
        String encodedName = this.componentConfig.getNamespace() + name;
        return (String[]) this.getParameterMap().get(encodedName);
    }

    public Object getAttribute(String name) {
        String encodedName = this.componentConfig.getNamespace() + name;
        Object value = super.getAttribute(encodedName);
        return value;
    }

    public void setAttribute(String name, Object value) {
        String encodedName = this.componentConfig.getNamespace() + name;
        super.setAttribute(encodedName, value);
    }

    public void removeAttribute(String name) {
        String encodedName = this.componentConfig.getNamespace() + name;
        super.removeAttribute(encodedName);
    }

}
