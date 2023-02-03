/*
 *  Copyright 2008-2023 Bloomreach
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
package org.hippoecm.hst.container;

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.HstContainerConfig;

public class HstContainerConfigImpl implements HstContainerConfig {
    
    private final ServletContext servletContext;
    private final ClassLoader contextClassLoader;
    
    public HstContainerConfigImpl(final ServletContext servletContext, final ClassLoader contextClassLoader) {
        this.servletContext = servletContext;
        this.contextClassLoader = contextClassLoader;
    }
    
    public ServletContext getServletContext() {
        return this.servletContext;
    }
    
    public ClassLoader getContextClassLoader() {
        return this.contextClassLoader;
    }
    
}
