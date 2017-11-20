/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.rest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.utils.inject.ApplicationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @version "$Id$"
 */
public class BaseResource {

    private static Logger log = LoggerFactory.getLogger(BaseResource.class);

    @Singleton
    @Inject
    private AutowireCapableBeanFactory injector;

    @Singleton
    @Inject
    private ApplicationContext applicationContext;


    public MessageRestful createErrorMessage(final String message, final HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
        return new ErrorMessageRestful(message);
    }

    public AutowireCapableBeanFactory getInjector() {
        if (injector == null) {
            if (applicationContext == null) {
                if (ApplicationModule.getApplicationContextRef() == null) {
                    // NOTE just a hack, avoids NPE, because it will inject beans, however, it is outside of current spring context :(
                    applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
                } else {
                    // TODO check if we can use  ContextLoader.getCurrentWebApplicationContext();
                    applicationContext = ApplicationModule.getApplicationContextRef();
                }
            }
            injector = applicationContext.getAutowireCapableBeanFactory();
        }
        return injector;
    }


    @PostConstruct
    public void init() {
        if (getInjector() != null) {
            injector.autowireBean(this);
        }
    }
}
