/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.autoreload;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.*;

public class AutoReloadServiceConfigTest {

    private AutoReloadServiceConfig config;

    @Before
    public void setUp() throws Exception {
        config = new AutoReloadServiceConfig();
    }

    @Test
    public void is_enabled_by_default() {
        assertTrue("auto-reload is enabled by default", config.isEnabled());
    }

    @Test
    public void can_be_reconfigured() throws RepositoryException {
        final Node newConfig = MockNode.root();
        newConfig.setProperty("enabled", false);
        config.reconfigure(newConfig);
        assertFalse("reconfigured auto-reload should be disabled", config.isEnabled());
    }

    @Test
    public void reconfiguration_uses_default_values_when_config_properties_are_missing() throws RepositoryException {
        final Node newEmptyConfig = MockNode.root();
        config.reconfigure(newEmptyConfig);
        assertTrue("reconfigured auto-reload with missing 'enabled' property should be enabled by default", config.isEnabled());
    }

}