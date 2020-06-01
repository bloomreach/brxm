/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.install;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.essentials.plugin.PluginSet;
import org.onehippo.cms7.essentials.sdk.api.model.rest.InstallState;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.test.ApplicationModule;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {ApplicationModule.class})
@ActiveProfiles("state-machine-test")
@Profile("state-machine-test")
@Configuration
public class InstallStateMachineTest {

    private static TestInstallService installService;
    private static final Map<String, Object> PARAMS = new HashMap<>();

    @Inject private InstallStateMachine stateMachine;

    @Bean
    @Primary
    public InstallService getTestInstallService() {
        installService = new TestInstallService();
        return installService;
    }

    @Test
    public void complete_install_no_deps() {
        final String id = "test";
        final UserFeedback feedback = new UserFeedback();
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setId(id);
        plugin.setName("Test Plugin");
        plugin.setState(InstallState.DISCOVERED);

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin);
        final PluginData pluginData = new PluginData(true, true, null);
        installService.pluginData.put(id, pluginData);

        assertTrue(stateMachine.installWithDependencies(id, pluginSet, PARAMS, feedback));

        assertEquals(InstallState.INSTALLING, plugin.getState());
        assertEquals(InstallState.INSTALLING, pluginData.persistedState);
        assertTrue(PARAMS == pluginData.installParameters);
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin' has been installed successfully, but requires a restart.",
                feedback.getFeedbackMessages().get(0).getMessage());
    }

    @Test
    public void staged_install_no_deps() {
        final String id = "test";
        final UserFeedback feedback = new UserFeedback();
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setId(id);
        plugin.setName("Test Plugin");
        plugin.setState(InstallState.DISCOVERED);
        plugin.setRebuildAfterInstallation(false);

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin);
        final PluginData pluginData = new PluginData(false, true, null);
        installService.pluginData.put(id, pluginData);

        // stage 1: block for user input
        assertTrue(stateMachine.installWithDependencies(id, pluginSet, PARAMS, feedback));

        assertEquals(InstallState.AWAITING_USER_INPUT, plugin.getState());
        assertEquals(InstallState.AWAITING_USER_INPUT, pluginData.persistedState);
        assertNull(pluginData.installParameters);
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin' requires user input for installation.",
                feedback.getFeedbackMessages().get(0).getMessage());

        // stage 2: installation
        assertTrue(stateMachine.installWithParameters(id, pluginSet, PARAMS, feedback));

        assertEquals(InstallState.INSTALLED, plugin.getState());
        assertEquals(InstallState.INSTALLED, pluginData.persistedState);
        assertTrue(PARAMS == pluginData.installParameters);
        assertEquals(2, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin' has been installed successfully.",
                feedback.getFeedbackMessages().get(1).getMessage());
    }

    @Test
    public void install_with_rebuild() {
        final String id = "test";
        final UserFeedback feedback = new UserFeedback();
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setId(id);
        plugin.setName("Test Plugin");
        plugin.setState(InstallState.DISCOVERED);

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin);
        final PluginData pluginData = new PluginData(true, true, null);
        installService.pluginData.put(id, pluginData);

        // stage 1: installation
        assertTrue(stateMachine.installWithDependencies(id, pluginSet, PARAMS, feedback));

        assertEquals(InstallState.INSTALLING, plugin.getState());
        assertEquals(InstallState.INSTALLING, pluginData.persistedState);
        assertTrue(PARAMS == pluginData.installParameters);
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin' has been installed successfully, but requires a restart.",
                feedback.getFeedbackMessages().get(0).getMessage());

        // restart without rebuild
        pluginData.installParameters = null;
        stateMachine.signalRestart(pluginSet, PARAMS, feedback);

        assertEquals(InstallState.INSTALLING, plugin.getState());
        assertEquals(InstallState.INSTALLING, pluginData.persistedState);
        assertNull(pluginData.installParameters);
        assertEquals(1, feedback.getFeedbackMessages().size());

        // rebuild and restart
        pluginData.warState = InstallState.INSTALLING;
        stateMachine.signalRestart(pluginSet, PARAMS, feedback);

        assertEquals(InstallState.INSTALLED, plugin.getState());
        assertEquals(InstallState.INSTALLED, pluginData.persistedState);
        assertNull(pluginData.installParameters);
        assertEquals(1, feedback.getFeedbackMessages().size());
    }

    @Test
    public void installation_fails() {
        final String id = "test";
        final UserFeedback feedback = new UserFeedback();
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setId(id);
        plugin.setName("Test Plugin");
        plugin.setState(InstallState.DISCOVERED);

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin);
        final PluginData pluginData = new PluginData(true, false, null);
        installService.pluginData.put(id, pluginData);

        assertTrue(stateMachine.installWithDependencies(id, pluginSet, PARAMS, feedback));
        assertEquals(InstallState.DISCOVERED, plugin.getState());
        assertNull(pluginData.persistedState);
        assertTrue(PARAMS == pluginData.installParameters);
        assertEquals(0, feedback.getFeedbackMessages().size());
    }

    @Test
    public void installation_in_wrong_state() {
        final String id = "test";
        final UserFeedback feedback = new UserFeedback();
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setId(id);
        plugin.setName("Test Plugin");
        plugin.setState(InstallState.DISCOVERED);

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin);

        assertFalse(stateMachine.installWithParameters(id, pluginSet, PARAMS, feedback));
    }

    @Test
    public void unknown_plugin_id() {
        final String id = "test";
        final UserFeedback feedback = new UserFeedback();
        final PluginSet pluginSet = new PluginSet();

        assertFalse(stateMachine.installWithDependencies(id, pluginSet, null, feedback));
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Failed to locate plugin with ID 'test'.", feedback.getFeedbackMessages().get(0).getMessage());

        assertFalse(stateMachine.installWithParameters(id, pluginSet, null, feedback));
        assertEquals(2, feedback.getFeedbackMessages().size());
        assertEquals("Failed to locate plugin with ID 'test'.", feedback.getFeedbackMessages().get(1).getMessage());
    }

    @Test
    public void unknown_dependency() {
        final UserFeedback feedback = new UserFeedback();
        final String id = "test";
        final PluginDescriptor.Dependency dependency = new PluginDescriptor.Dependency();
        dependency.setPluginId("unknown");
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setId(id);
        plugin.setName("Test Plugin");
        plugin.setPluginDependencies(Collections.singletonList(dependency));
        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin);

        assertFalse(stateMachine.installWithDependencies(id, pluginSet, null, feedback));
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Failed to locate plugin with ID 'unknown'.", feedback.getFeedbackMessages().get(0).getMessage());

        assertFalse(stateMachine.installWithParameters(id, pluginSet, null, feedback));
        assertEquals(2, feedback.getFeedbackMessages().size());
        assertEquals("Failed to locate plugin with ID 'unknown'.", feedback.getFeedbackMessages().get(1).getMessage());
    }

    @Test
    public void trivial_circular_dependency() {
        final UserFeedback feedback = new UserFeedback();
        final String id = "test";
        final PluginDescriptor.Dependency dependency = new PluginDescriptor.Dependency();
        dependency.setPluginId(id);
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setId(id);
        plugin.setName("Test Plugin");
        plugin.setPluginDependencies(Collections.singletonList(dependency));
        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin);

        // with default parameters
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(InstallStateMachine.class).build()) {
            assertFalse(stateMachine.installWithDependencies(id, pluginSet, null, feedback));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Dependency chain for cyclic plugin dependency is: 'Test Plugin' -> 'Test Plugin'.")));
        }
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin' has cyclic dependency, unable to install. Check back-end logs.",
                feedback.getFeedbackMessages().get(0).getMessage());

        // with user input
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(InstallStateMachine.class).build()) {
            assertFalse(stateMachine.installWithParameters(id, pluginSet, null, feedback));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Dependency chain for cyclic plugin dependency is: 'Test Plugin' -> 'Test Plugin'.")));
        }
        assertEquals(2, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin' has cyclic dependency, unable to install. Check back-end logs.",
                feedback.getFeedbackMessages().get(1).getMessage());
    }

    @Test
    public void deep_circular_dependency() {
        final UserFeedback feedback = new UserFeedback();
        final String id1 = "test1";
        final String id2 = "test2";
        final PluginDescriptor.Dependency dependency1 = new PluginDescriptor.Dependency();
        dependency1.setPluginId(id2);
        final PluginDescriptor plugin1 = new PluginDescriptor();
        plugin1.setId(id1);
        plugin1.setName("Test Plugin 1");
        plugin1.setPluginDependencies(Collections.singletonList(dependency1));

        final PluginDescriptor.Dependency dependency2 = new PluginDescriptor.Dependency();
        dependency2.setPluginId(id1);
        final PluginDescriptor plugin2 = new PluginDescriptor();
        plugin2.setId(id2);
        plugin2.setName("Test Plugin 2");
        plugin2.setPluginDependencies(Collections.singletonList(dependency2));

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin1);
        pluginSet.add(plugin2);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(InstallStateMachine.class).build()) {
            assertFalse(stateMachine.installWithDependencies(id1, pluginSet, null, feedback));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Dependency chain for cyclic plugin dependency is: 'Test Plugin 1' -> 'Test Plugin 2' -> 'Test Plugin 1'.")));
        }
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin 1' has cyclic dependency, unable to install. Check back-end logs.",
                feedback.getFeedbackMessages().get(0).getMessage());
    }

    @Test
    public void complex_circular_dependency() {
        final UserFeedback feedback = new UserFeedback();
        final String id1 = "test1";
        final String id2 = "test2";
        final String id3 = "test3";

        final PluginDescriptor.Dependency dependsOn1 = new PluginDescriptor.Dependency();
        dependsOn1.setPluginId(id1);
        final PluginDescriptor.Dependency dependsOn2 = new PluginDescriptor.Dependency();
        dependsOn2.setPluginId(id2);
        final PluginDescriptor.Dependency dependsOn3 = new PluginDescriptor.Dependency();
        dependsOn3.setPluginId(id3);

        final PluginDescriptor plugin1 = new PluginDescriptor();
        plugin1.setId(id1);
        plugin1.setName("Test Plugin 1");
        plugin1.setPluginDependencies(Arrays.asList(dependsOn2, dependsOn3));

        final PluginDescriptor plugin2 = new PluginDescriptor();
        plugin2.setId(id2);
        plugin2.setName("Test Plugin 2");
        plugin2.setPluginDependencies(Collections.singletonList(dependsOn3));

        final PluginDescriptor plugin3 = new PluginDescriptor();
        plugin3.setId(id3);
        plugin3.setName("Test Plugin 3");
        plugin3.setPluginDependencies(Collections.singletonList(dependsOn1));

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin1);
        pluginSet.add(plugin2);
        pluginSet.add(plugin3);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(InstallStateMachine.class).build()) {
            assertFalse(stateMachine.installWithDependencies(id1, pluginSet, null, feedback));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Dependency chain for cyclic plugin dependency is: 'Test Plugin 1' -> 'Test Plugin 2' -> 'Test Plugin 3' -> 'Test Plugin 1'.")));
        }
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin 1' has cyclic dependency, unable to install. Check back-end logs.",
                feedback.getFeedbackMessages().get(0).getMessage());
    }

    @Test
    public void dependency_on_full_installation() {
        final String id1 = "test1";
        final String id2 = "test2";

        final PluginDescriptor.Dependency dependency = new PluginDescriptor.Dependency();
        dependency.setPluginId(id2);
        dependency.setMinInstallStateForInstalling(InstallState.INSTALLED.toString());

        final PluginDescriptor plugin1 = new PluginDescriptor();
        plugin1.setId(id1);
        plugin1.setName("Test Plugin 1");
        plugin1.setState(InstallState.DISCOVERED);
        plugin1.setRebuildAfterInstallation(false);
        plugin1.setPluginDependencies(Collections.singletonList(dependency));

        final PluginDescriptor plugin2 = new PluginDescriptor();
        plugin2.setId(id2);
        plugin2.setName("Test Plugin 2");
        plugin2.setState(InstallState.DISCOVERED);

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin1);
        pluginSet.add(plugin2);

        final PluginData data1 = new PluginData(true, true, null);
        installService.pluginData.put(id1, data1);
        final PluginData data2 = new PluginData(true, true, null);
        installService.pluginData.put(id2, data2);

        // stage 1, installation of dependent plugin is triggered, and stalls in INSTALLING state.
        //          installation of requested plugin waits in INSTALLATION_PENDING, waiting for dependent plugin.
        final UserFeedback feedback1 = new UserFeedback();
        assertTrue(stateMachine.installWithDependencies(id1, pluginSet, PARAMS, feedback1));
        assertEquals(InstallState.INSTALLATION_PENDING, plugin1.getState());
        assertEquals(InstallState.INSTALLING, plugin2.getState());
        assertNull(data1.installParameters);
        assertTrue(PARAMS == data2.installParameters);
        assertEquals(3, feedback1.getFeedbackMessages().size());
        assertEquals("Installing dependent plugin 'Test Plugin 2'...", feedback1.getFeedbackMessages().get(0).getMessage());
        assertEquals("Plugin 'Test Plugin 2' has been installed successfully, but requires a restart.",
                feedback1.getFeedbackMessages().get(1).getMessage());
        assertEquals("Installation of plugin 'Test Plugin 1' is waiting for the installation of dependent plugin(s) 'Test Plugin 2'.",
                feedback1.getFeedbackMessages().get(2).getMessage());

        // stage 2, simulate a rebuild + restart
        data2.warState = plugin2.getState();
        final UserFeedback feedback2 = new UserFeedback();
        stateMachine.signalRestart(pluginSet, PARAMS, feedback2);
        // installation of dependent plugin advances into INSTALLING state, requested plugin remains in pending state,
        // waiting for full installation of the dependency
        assertEquals(InstallState.INSTALLED, plugin1.getState());
        assertEquals(InstallState.INSTALLED, plugin2.getState());
        assertTrue(PARAMS == data1.installParameters);
        assertTrue(PARAMS == data2.installParameters);
        assertEquals(1, feedback2.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin 1' has been installed successfully.",
                feedback2.getFeedbackMessages().get(0).getMessage());
    }

    @Test
    public void cascading_dependency() {
        // plugin1's installation depends on plugin2 being installed,
        // while plugin2's installation depends on plugin3 being installing.
        // no plugin requires a rebuild+restart, but plugin2 requires installation parameters.
        final String id1 = "test1";
        final String id2 = "test2";
        final String id3 = "test3";

        final PluginDescriptor.Dependency dependsOn2 = new PluginDescriptor.Dependency();
        dependsOn2.setPluginId(id2);
        dependsOn2.setMinInstallStateForInstalling(InstallState.INSTALLING.toString());
        final PluginDescriptor.Dependency dependsOn3 = new PluginDescriptor.Dependency();
        dependsOn3.setPluginId(id3);
        dependsOn3.setMinInstallStateForInstalling(InstallState.INSTALLED.toString());

        final PluginDescriptor plugin1 = new PluginDescriptor();
        plugin1.setId(id1);
        plugin1.setName("Test Plugin 1");
        plugin1.setState(InstallState.DISCOVERED);
        plugin1.setRebuildAfterInstallation(false);
        plugin1.setPluginDependencies(Collections.singletonList(dependsOn2));

        final PluginDescriptor plugin2 = new PluginDescriptor();
        plugin2.setId(id2);
        plugin2.setName("Test Plugin 2");
        plugin2.setState(InstallState.DISCOVERED);
        plugin2.setRebuildAfterInstallation(false);
        plugin2.setPluginDependencies(Collections.singletonList(dependsOn3));

        final PluginDescriptor plugin3 = new PluginDescriptor();
        plugin3.setId(id3);
        plugin3.setName("Test Plugin 3");
        plugin3.setState(InstallState.DISCOVERED);
        plugin3.setRebuildAfterInstallation(false);

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin1);
        pluginSet.add(plugin2);
        pluginSet.add(plugin3);

        final PluginData data1 = new PluginData(true, true, null);
        installService.pluginData.put(id1, data1);
        final PluginData data2 = new PluginData(false, true, null);
        installService.pluginData.put(id2, data2);
        final PluginData data3 = new PluginData(true, true, null);
        installService.pluginData.put(id3, data3);

        final UserFeedback feedback1 = new UserFeedback();
        assertTrue(stateMachine.installWithDependencies(id1, pluginSet, PARAMS, feedback1));
        assertEquals(InstallState.INSTALLATION_PENDING, plugin1.getState());
        assertEquals(InstallState.AWAITING_USER_INPUT, plugin2.getState());
        assertEquals(InstallState.INSTALLED, plugin3.getState());
        assertEquals(5, feedback1.getFeedbackMessages().size());
        assertEquals("Installing dependent plugin 'Test Plugin 2'...",
                feedback1.getFeedbackMessages().get(0).getMessage());
        assertEquals("Installing dependent plugin 'Test Plugin 3'...",
                feedback1.getFeedbackMessages().get(1).getMessage());
        assertEquals("Plugin 'Test Plugin 3' has been installed successfully.",
                feedback1.getFeedbackMessages().get(2).getMessage());
        assertEquals("Plugin 'Test Plugin 2' requires user input for installation.",
                feedback1.getFeedbackMessages().get(3).getMessage());
        assertEquals("Installation of plugin 'Test Plugin 1' is waiting for the installation of dependent plugin(s) 'Test Plugin 2'.",
                feedback1.getFeedbackMessages().get(4).getMessage());

        // we need to explicitly trigger the installation of plugin2
        final UserFeedback feedback2 = new UserFeedback();
        assertTrue(stateMachine.installWithParameters(id2, pluginSet, PARAMS, feedback2));
        assertEquals(InstallState.INSTALLED, plugin1.getState());
        assertEquals(InstallState.INSTALLED, plugin2.getState());
        assertEquals(InstallState.INSTALLED, plugin3.getState());
        assertEquals(2, feedback2.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin 2' has been installed successfully.",
                feedback2.getFeedbackMessages().get(0).getMessage());
        assertEquals("Plugin 'Test Plugin 1' has been installed successfully.",
                feedback2.getFeedbackMessages().get(1).getMessage());
    }

    @Test
    public void empty_dependency() {
        // this is a little contrived: if a dependency specifies no minimal state for installation,
        // it effectively gets not installed.

        final String id1 = "test1";
        final String id2 = "test2";

        final PluginDescriptor.Dependency dependency = new PluginDescriptor.Dependency();
        dependency.setPluginId(id2);
        dependency.setMinInstallStateForInstalling(null);

        final PluginDescriptor plugin1 = new PluginDescriptor();
        plugin1.setId(id1);
        plugin1.setName("Test Plugin 1");
        plugin1.setState(InstallState.DISCOVERED);
        plugin1.setRebuildAfterInstallation(false);
        plugin1.setPluginDependencies(Collections.singletonList(dependency));

        final PluginDescriptor plugin2 = new PluginDescriptor();
        plugin2.setId(id2);
        plugin2.setName("Test Plugin 2");
        plugin2.setState(InstallState.DISCOVERED);
        plugin2.setRebuildAfterInstallation(false);

        final PluginSet pluginSet = new PluginSet();
        pluginSet.add(plugin1);
        pluginSet.add(plugin2);

        final PluginData data1 = new PluginData(true, true, null);
        installService.pluginData.put(id1, data1);
        final PluginData data2 = new PluginData(true, true, null);
        installService.pluginData.put(id2, data2);

        final UserFeedback feedback = new UserFeedback();
        assertTrue(stateMachine.installWithDependencies(id1, pluginSet, PARAMS, feedback));
        assertEquals(InstallState.INSTALLED, plugin1.getState());
        assertEquals(InstallState.DISCOVERED, plugin2.getState());
        assertTrue(PARAMS == data1.installParameters);
        assertNull(data2.installParameters);
        assertEquals(1, feedback.getFeedbackMessages().size());
        assertEquals("Plugin 'Test Plugin 1' has been installed successfully.",
                feedback.getFeedbackMessages().get(0).getMessage());
    }


    private static class TestInstallService extends InstallService {
        Map<String, PluginData> pluginData = new HashMap<>();

        public TestInstallService() {
            super(null, null, null, null, null);
        }

        @Override
        public InstallState readInstallStateFromWar(final PluginDescriptor plugin) {
            final PluginData data = pluginData.get(plugin.getId());
            return data.warState;
        }

        @Override
        public void storeInstallStateToFileSystem(final PluginDescriptor plugin) {
            final PluginData data = pluginData.get(plugin.getId());
            data.persistedState = plugin.getState();
        }

        @Override
        public boolean canAutoInstall(final PluginDescriptor plugin) {
            final PluginData data = pluginData.get(plugin.getId());
            return data.canAutoInstall;
        }

        @Override
        public boolean install(final PluginDescriptor plugin, final Map<String, Object> parameters) {
            final PluginData data = pluginData.get(plugin.getId());
            data.installParameters = parameters;
            return data.installationResult;
        }
    }

    private static class PluginData {
        boolean canAutoInstall = true;
        boolean installationResult = true;
        InstallState warState = InstallState.DISCOVERED;
        InstallState persistedState;
        Map<String, Object> installParameters;

        PluginData(boolean canAutoInstall, boolean installationResult, InstallState warState) {
            this.canAutoInstall = canAutoInstall;
            this.installationResult = installationResult;
            this.warState = warState;
        }
    }
}
