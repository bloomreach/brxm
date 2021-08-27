/*
 *  Copyright 2021 Bloomreach
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
package org.bloomreach.xm.cms;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.util.StringUtils;

public class ServletInitializer extends SpringBootServletInitializer {

    private static final String LOGGING_SYSTEM_PROPERTY = LoggingSystem.SYSTEM_PROPERTY;
    private static final String LOGGING_SYSTEM_CLASS = XmLoggingSystem.class.getName();

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        if (!StringUtils.hasLength(System.getProperty(LOGGING_SYSTEM_PROPERTY))) {
            // set the XmLoggingSystem class as a system property hence spring boot can use it
            // as the default LoggingSystem implementation
            System.setProperty(LOGGING_SYSTEM_PROPERTY, LOGGING_SYSTEM_CLASS);
        }
        return application.sources(XmApplication.class);
    }
}