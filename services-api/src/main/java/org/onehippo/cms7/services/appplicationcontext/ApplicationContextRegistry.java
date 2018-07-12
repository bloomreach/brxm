/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.appplicationcontext;

import org.onehippo.cms7.services.HippoServiceException;
import org.onehippo.cms7.services.ServiceHolder;
import org.onehippo.cms7.services.WhiteboardServiceRegistry;

public class ApplicationContextRegistry extends WhiteboardServiceRegistry<ApplicationContext> {

    private static final ApplicationContextRegistry INSTANCE = new ApplicationContextRegistry();

    private ApplicationContextRegistry() {
    }

    public static ApplicationContextRegistry get() {
        return INSTANCE;
    }

    @Override
    public synchronized void register(final ApplicationContext serviceObject) throws HippoServiceException {
        String contextPath = serviceObject.getServletContext().getContextPath();
        for (ServiceHolder<ApplicationContext> holder: getEntriesList()) {
            if (contextPath.equals(holder.getServiceObject().getServletContext().getContextPath())) {
                throw new HippoServiceException("ApplicationContext with context path "+contextPath+" already registered");
            }
        }
        super.register(serviceObject);
    }
}
