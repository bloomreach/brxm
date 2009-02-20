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
