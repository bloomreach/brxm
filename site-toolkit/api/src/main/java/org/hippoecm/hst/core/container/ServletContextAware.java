/*
 *  Copyright 2010 Hippo.
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

import javax.servlet.ServletContext;

/**
 * <p>
 * Interface to be implemented by a {@link ComponentManager} object that wishes to be notified of the ServletContext that it runs in.
 * </p>
 * <p>
 * Note that this {@link ServletContextAware} is meant to make a  {@link ComponentManager} object ServletContext aware and <b>not</b>
 * its containing Spring beans! To make a spring bean {@link ServletContext} aware, such a spring bean need to implement 
 * org.springframework.web.context.ServletContextAware
 * </p>
 * @version $Id$
 */
public interface ServletContextAware {
    
    /**
     * Set the ServletContext that this object runs in.
     * @param servletContext
     */
    void setServletContext(ServletContext servletContext);     
}
