/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.backend;

import java.util.Map;

import javax.jcr.Session;

import org.onehippo.cm.api.ConfigurationService;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private final Session session;

    public ConfigurationServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public void apply(final MergedModel mergedModel, final Map<Module, ResourceInputProvider> resourceInputProviders)
            throws Exception {
        try {
            final ApplyConfigurationHelper helper = new ApplyConfigurationHelper(session, resourceInputProviders);
            helper.push(mergedModel);
            session.save();
        } catch (Exception e) {
            log.warn("Failed to apply configuration", e);
            throw e;
        }
    }

}
