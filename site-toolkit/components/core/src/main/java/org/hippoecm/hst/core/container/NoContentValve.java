/*
 *  Copyright 2011 Hippo.
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

import javax.servlet.http.HttpServletResponse;

/**
 * NoContentValve 
 * When this valve is used, it does not make sense to also have valves that write content to the {@link HttpServletResponse}
 * since this valve sets servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT)
 */
public class NoContentValve implements Valve {

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HttpServletResponse servletResponse = context.getServletResponse();
        servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
        context.invokeNext();
    }

    @Override
    public void initialize() throws ContainerException {
        
    }

    @Override
    public void destroy() {
        
    }
}
