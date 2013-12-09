/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms7.essentials.rest.BeanWriterResource;
import org.onehippo.cms7.essentials.rest.MenuResource;
import org.onehippo.cms7.essentials.rest.PluginResource;
import org.onehippo.cms7.essentials.rest.PowerpackResource;
import org.onehippo.cms7.essentials.rest.PropertiesResource;
import org.onehippo.cms7.essentials.rest.StatusResource;
import org.onehippo.cms7.essentials.rest.model.StatusRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.inject.jaxrs.CXFServerModule;
import com.google.inject.Provides;

/**
 * @version "$Id$"
 */
public class RestModule extends CXFServerModule {

    private static Logger log = LoggerFactory.getLogger(RestModule.class);

    @Override
    protected void configure() {
        publish(MenuResource.class);
        publish(PluginResource.class);
        publish(StatusResource.class);
        publish(PowerpackResource.class);
        publish(PropertiesResource.class);
        publish(BeanWriterResource.class);
    }


}
