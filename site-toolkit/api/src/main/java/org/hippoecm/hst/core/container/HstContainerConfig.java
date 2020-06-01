/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import javax.servlet.ServletContext;

/**
 * The HstComponent container configuration.
 * Because the container's request processor can be located in other web application
 * and loaded by other class loader for centralized management reason,
 * The HstComponent container servlet should pass an implementation of this interface
 * to the request processor. 
 * 
 * @version $Id$
 */
public interface HstContainerConfig {

    /**
     * Returns the servletContext of the web application where the HstComponents are located.
     * @return the servletContext of the web application where the HstComponents are located.
     */
    ServletContext getServletContext();
    
    /**
     * Returns the classloader of the web application where the HstComponents are located.
     * @return the classloader of the web application where the HstComponents are located.
     */
    ClassLoader getContextClassLoader();
    
}
