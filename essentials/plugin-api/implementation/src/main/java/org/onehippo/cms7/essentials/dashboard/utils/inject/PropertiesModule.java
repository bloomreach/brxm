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

package org.onehippo.cms7.essentials.dashboard.utils.inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * @version "$Id$"
 */
public class PropertiesModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(PropertiesModule.class);

    @Override
    protected void configure() {
        final Properties properties = new Properties();
        try (final InputStream stream = getClass().getResourceAsStream("/essentials_messages.properties")) {
            properties.load(stream);
            Names.bindProperties(binder(), properties);
        } catch (IOException e) {
            log.error("Error injecting properties [essentials_messages.properties]", e);
        }
    }
}
