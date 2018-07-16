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
package org.onehippo.cms7.services;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;

/**
 * Register web application's ServletContext in HippoWebappContextRegistry
 */
public class HippoWebappListener implements ServletContextListener {

    private HippoWebappContext context;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        context = new HippoWebappContext(HippoWebappContext.Type.HST, sce.getServletContext());
        HippoWebappContextRegistry.get().register(context);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        HippoWebappContextRegistry.get().unregister(context);
    }
}