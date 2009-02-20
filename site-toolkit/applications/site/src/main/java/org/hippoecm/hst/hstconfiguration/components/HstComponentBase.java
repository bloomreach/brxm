package org.hippoecm.hst.hstconfiguration.components;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfigurationBean;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class HstComponentBase implements HstComponent {
    
    protected String name;
    protected HstComponentConfigurationBean hstComponentConfigurationBean;
    
    public HstComponentBase() {
    }
    
    public String getName() {
        if (this.name == null) {
            this.name = getClass().getName();
        }
        
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public HstComponentConfigurationBean getHstComponentConfigurationBean(){
        return this.hstComponentConfigurationBean;
    }
    
    public void init(ServletConfig servletConfig, HstComponentConfigurationBean compConfig) throws HstComponentException {
        this.hstComponentConfigurationBean = compConfig;
        System.out.println("[HstComponent: " + getName() + "] init()");
    }

    public void destroy() throws HstComponentException {
        System.out.println("[HstComponent: " + getName() + "] destroy()");
    }

    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        System.out.println("[HstComponent: " + getName() + "] doAction()");
    }

    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        System.out.println("[HstComponent: " + getName() + "] doBeforeRender()");
    }

    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        System.out.println("[HstComponent: " + getName() + "] doBeforeServeResource()");
    }
}
