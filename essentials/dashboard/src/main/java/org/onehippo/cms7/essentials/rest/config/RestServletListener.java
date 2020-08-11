/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestServletListener implements ServletContextListener {

    private static Logger log = LoggerFactory.getLogger(RestServletListener.class);

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        log.info("#####  ESSENTIALS LISTENER #############");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        // nothing to destroy
    }
}
