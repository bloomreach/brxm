/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.servlet;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.inject.ApplicationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a JAXRS Application which instantiates and Spring-wires resource classes as singletons.
 */
@ApplicationPath("/dynamic")
public class DynamicRestPointsApplication extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicRestPointsApplication.class);
    private final Set<String> fqcns = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    public void addSingleton(final String fqcn) {
        if (fqcns.contains(fqcn)) {
            return;
        }

        final Object singleton = GlobalUtils.newInstance(fqcn);
        if (singleton != null) {
            LOG.info("Add dynamic (plugin) REST endpoint '{}'.", fqcn);

            ApplicationModule.getInjector().autowireBean(singleton);
            fqcns.add(fqcn);
            singletons.add(singleton);
        }
    }
}
