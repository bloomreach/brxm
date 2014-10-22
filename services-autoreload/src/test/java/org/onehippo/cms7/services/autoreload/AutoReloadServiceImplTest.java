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

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AutoReloadServiceImplTest {

    public static final String DUMMY_JAVASCRIPT = "/* dummy JavaScript */";
    private AutoReloadServiceConfig config;
    private AutoReloadServer autoReloadServer;

    @Before
    public void setUp() throws Exception {
        config = new AutoReloadServiceConfig();
        autoReloadServer = EasyMock.createNiceMock(AutoReloadServer.class);
    }

    private AutoReloadServiceImpl createAutoReloadServiceImpl(final String autoReloadScript) {
        AutoReloadScriptLoader scriptLoader = EasyMock.createNiceMock(AutoReloadScriptLoader.class);
        expect(scriptLoader.getJavaScript()).andReturn(autoReloadScript);
        replay(scriptLoader);
        return new AutoReloadServiceImpl(config, scriptLoader, autoReloadServer);
    }

    @Test
    public void uses_javascript_from_loader() {
        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(DUMMY_JAVASCRIPT);
        assertEquals(DUMMY_JAVASCRIPT, autoReload.getJavaScript());
    }

    @Test
    public void uses_null_javascript_when_loading_fails() {
        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(null);
        assertNull(autoReload.getJavaScript());
    }

    @Test
    public void is_enabled_with_default_config_and_loaded_script() {
        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(DUMMY_JAVASCRIPT);
        assertTrue(autoReload.isEnabled());
    }

    @Test
    public void is_disabled_with_default_config_and_missing_script() {
        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(null);
        assertFalse(autoReload.isEnabled());
    }

    @Test
    public void is_disabled_with_changed_config_and_loaded_script() throws RepositoryException {
        Node changedConfig = MockNode.root();
        changedConfig.setProperty("enabled", false);
        config.reconfigure(changedConfig);

        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(DUMMY_JAVASCRIPT);
        assertFalse(autoReload.isEnabled());
    }

    @Test
    public void is_disabled_when_explicitly_told_so() throws RepositoryException {
        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(DUMMY_JAVASCRIPT);
        autoReload.setEnabled(false);
        assertFalse(autoReload.isEnabled());
    }

    @Test
    public void is_disabled_when_explicitly_enabled_but_script_is_missing() throws RepositoryException {
        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(null);
        autoReload.setEnabled(true);
        assertFalse(autoReload.isEnabled());
    }

    @Test
    public void is_disabled_when_explicitly_enabled_but_config_is_disabled() throws RepositoryException {
        Node changedConfig = MockNode.root();
        changedConfig.setProperty("enabled", false);
        config.reconfigure(changedConfig);

        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(DUMMY_JAVASCRIPT);
        autoReload.setEnabled(true);
        assertFalse(autoReload.isEnabled());
    }

    @Test
    public void returns_javascript_when_disabled() throws RepositoryException {
        String script = DUMMY_JAVASCRIPT;
        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(script);
        autoReload.setEnabled(false);
        assertEquals(script, autoReload.getJavaScript());
    }

    @Test
    public void broadcast_page_reload_when_enabled() throws RepositoryException {
        autoReloadServer.broadcastPageReload();
        expectLastCall();
        replay(autoReloadServer);

        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(DUMMY_JAVASCRIPT);
        autoReload.broadcastPageReload();

        verify(autoReloadServer);
    }

    @Test
    public void does_not_broadcast_page_reload_when_disabled() throws RepositoryException {
        /**
         * use normal mock instead of nice mock to let it throw an error when
         * {@link AutoReloadServer#broadcastPageReload} is called
         */
        autoReloadServer = EasyMock.createMock(AutoReloadServer.class);
        replay(autoReloadServer);

        AutoReloadServiceImpl autoReload = createAutoReloadServiceImpl(DUMMY_JAVASCRIPT);
        autoReload.setEnabled(false);
        autoReload.broadcastPageReload();

        verify(autoReloadServer);
    }

}