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
package org.hippoecm.hst.core.component;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>GenericHstComponent</code> class provides a default implementation for
 * the <code>HstComponent</code> interface. 
 * <p/>
 * It provides a base class to be subclassed to create HstComponents. Common practice is that 
 * {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration}'s whose only purpose is layout (markup) and do not have
 * actual behavior, use this default <code>HstComponent</code> implementation as their component classname, 
 * see {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration#getComponentClassName()}.
 * 
 * <p/>
 * A subclass of <code>GenericHstComponent</code> can override methods, usually
 * one of the following:
 * <ul>
 * <li>doAction, to handle action requests</li>
 * <li>doBeforeRender, to retrieve model objects from the repository to be passed to the rendering page or servlet</li>
 * <li>doBeforeServeResource, to retrieve model objects from the repository to be passed to the resource serving page or servlet</li>
 * <li>init and destroy, to manage resources that are held for the life of the HstComponent</li>
 * </ul>
 * <strong>NOTE:</strong> <code>HstComponent</code>'s typically run on multithreaded servers, so take care that a
 * HstComponent must handle concurrent requests and be careful to synchronize access
 * to shared resources (in other words, be thread safe, see {@link HstComponent}). Shared resources include in-memory data such as 
 * instance or class variables and external objects such as files, database connections, and network connections. 
 * 
 * @version $Id$
 */
public class GenericHstComponent implements HstComponent {

    private static final Logger log = LoggerFactory.getLogger(GenericHstComponent.class);

    /** Configuration key for flag whether or not to allow resource path resolving by resourceID as fallback. */
    public static final String RESOURCE_PATH_BY_RESOURCE_ID = "org.hippoecm.hst.core.component.serveResourcePathByResourceID";

    private ComponentConfiguration componentConfig;

    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException {
        this.componentConfig = componentConfig;
    }
    
    public void destroy() throws HstComponentException {

    }

    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {

    }

    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

    }

    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        if (componentConfig.getServeResourcePath() == null) {
            String resourceID = request.getResourceID();

            if (resourceID != null && !resourceID.equals("")) {
                if (request.getRequestContext().getContainerConfiguration().getBoolean(RESOURCE_PATH_BY_RESOURCE_ID, false)) {
                    if (resourceID.endsWith(".jsp") || resourceID.endsWith(".ftl")) {
                        response.setServeResourcePath(resourceID);
                    } else {
                        log.warn("ResourceID for serveResourcePath as fallback is valid only when it is .jsp or .ftl. Return 404.");
                        try {
                            response.sendError(response.SC_NOT_FOUND);
                        } catch (IOException e) {
                             throw new HstComponentException("Unable to set 404 on response after invalid resource path.", e);
                        }
                    }
                }
            }
        }
    }

    protected ComponentConfiguration getComponentConfiguration() {
        return this.componentConfig;
    }
}
