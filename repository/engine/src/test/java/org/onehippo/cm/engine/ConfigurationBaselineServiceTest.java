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

package org.onehippo.cm.engine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.parser.ActionListParser;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_NODE_PATH;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_PATHS_APPLIED;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_SEQUENCE;
import static org.onehippo.cm.engine.Constants.HCM_ROOT;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;
import static org.onehippo.cm.engine.Constants.HCM_YAML;
import static org.onehippo.cm.engine.Constants.NT_HCM_BASELINE;
import static org.onehippo.cm.engine.Constants.NT_HCM_CONTENT;
import static org.onehippo.cm.engine.Constants.NT_HCM_DESCRIPTOR;
import static org.onehippo.cm.engine.Constants.NT_HCM_GROUP;
import static org.onehippo.cm.engine.Constants.NT_HCM_MODULE;
import static org.onehippo.cm.engine.Constants.NT_HCM_PROJECT;
import static org.onehippo.cm.engine.Constants.NT_HCM_ROOT;
import static org.onehippo.cm.model.Constants.HCM_MODULE_YAML;

public class ConfigurationBaselineServiceTest extends BaseConfigurationConfigServiceTest {

    private ConfigurationBaselineService baselineService;
    private ConfigurationLockManager configurationLockManager;

    @Before
    public void load_hcm_cnd() throws Exception {
        final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        try {
            namespaceRegistry.getURI("hcm");
        } catch (NamespaceException e) {
            namespaceRegistry.registerNamespace("hcm", "http://www.onehippo.org/jcr/hcm/1.0");
        }

        final InputStream cndStream = getClass().getClassLoader().getResourceAsStream("hcm-config/hcm.cnd");
        BootstrapUtils.initializeNodetypes(session, cndStream, "hcm.cnd");
        session.save();
        cndStream.close();

        configurationLockManager = new ConfigurationLockManager(session);
        baselineService = new ConfigurationBaselineService(session, configurationLockManager);
    }

    @After
    public void cleanup_baseline_nodes() throws Exception {
        configurationLockManager.stop();
        if (session.nodeExists(HCM_ROOT_PATH)) {
            session.getNode(HCM_ROOT_PATH).remove();
            session.save();
        }
    }

    @Test
    public void expect_applied_paths_to_be_read() throws Exception {
        final Node hcmRoot = session.getRootNode().addNode(HCM_ROOT, NT_HCM_ROOT);
        final Node hcmContent = hcmRoot.addNode(NT_HCM_CONTENT, NT_HCM_CONTENT);
        hcmContent.setProperty(HCM_CONTENT_PATHS_APPLIED, new String[] {"/a/b", "/c", "/a/d/e"});
        session.save();

        final List<String> appliedPaths = new ArrayList<>(baselineService.getAppliedContentPaths());

        assertEquals(3, appliedPaths.size());
        assertEquals("/a/b", appliedPaths.get(0));
        assertEquals("/c", appliedPaths.get(1));
        assertEquals("/a/d/e", appliedPaths.get(2));
    }

    @Test
    public void expect_empty_set_if_root_node_missing() throws Exception {
        assertTrue(baselineService.getAppliedContentPaths().isEmpty());
    }

    @Test
    public void expect_new_applied_path_to_be_added_last() throws Exception {
        final Node hcmRoot = session.getRootNode().addNode(HCM_ROOT, NT_HCM_ROOT);
        final Node hcmContent = hcmRoot.addNode(NT_HCM_CONTENT, NT_HCM_CONTENT);
        hcmContent.setProperty(HCM_CONTENT_PATHS_APPLIED, new String[] {"/a/b", "/c", "/a/d/e"});
        session.save();

        baselineService.addAppliedContentPath("/a/a");
        final List<String> appliedPaths = new ArrayList<>(baselineService.getAppliedContentPaths());

        assertEquals(4, appliedPaths.size());
        assertEquals("/a/b", appliedPaths.get(0));
        assertEquals("/c", appliedPaths.get(1));
        assertEquals("/a/d/e", appliedPaths.get(2));
        assertEquals("/a/a", appliedPaths.get(3));
    }

    @Test
    public void expect_existing_applied_path_not_to_be_added() throws Exception {
        final Node hcmRoot = session.getRootNode().addNode(HCM_ROOT, NT_HCM_ROOT);
        final Node hcmContent = hcmRoot.addNode(NT_HCM_CONTENT, NT_HCM_CONTENT);
        hcmContent.setProperty(HCM_CONTENT_PATHS_APPLIED, new String[] {"/a/b", "/c", "/a/d/e"});
        session.save();

        baselineService.addAppliedContentPath("/c");
        final List<String> appliedPaths = new ArrayList<>(baselineService.getAppliedContentPaths());

        assertEquals(3, appliedPaths.size());
        assertEquals("/a/b", appliedPaths.get(0));
        assertEquals("/c", appliedPaths.get(1));
        assertEquals("/a/d/e", appliedPaths.get(2));
    }

    @Test
    public void expect_baseline_nodes_to_be_created_when_addeing_applied_path() throws Exception {
        baselineService.addAppliedContentPath("/c");
        final List<String> appliedPaths = new ArrayList<>(baselineService.getAppliedContentPaths());

        assertEquals(1, appliedPaths.size());
        assertEquals("/c", appliedPaths.get(0));

        assertTrue(session.nodeExists(HCM_CONTENT_NODE_PATH));
    }

    @Test
    public void expect_sequence_number_not_updated_if_no_actions() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /foo:\n"
                + "        jcr:primaryType: nt:unstructured";
        final ConfigurationModelImpl baseline = applyDefinitions(baselineSource);
        final ModuleImpl module = baseline.getModulesStream().findFirst().get();

        assertEquals(null, module.getSequenceNumber());

        baselineService.updateModuleSequenceNumber(module);
    }

    @Test
    public void expect_sequence_number_to_be_updated() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /foo:\n"
                + "        jcr:primaryType: nt:unstructured";
        final ConfigurationModelImpl baseline = applyDefinitions(baselineSource);
        final ModuleImpl module = baseline.getModulesStream().findFirst().get();
        final ActionListParser actionListParser = new ActionListParser();

        final Node moduleBaseline = session.getRootNode()
                .addNode(HCM_ROOT, NT_HCM_ROOT)
                .addNode(NT_HCM_BASELINE, NT_HCM_BASELINE)
                .addNode("test-group", NT_HCM_GROUP)
                .addNode("test-project", NT_HCM_PROJECT)
                .addNode("test-module-0", NT_HCM_MODULE);
        moduleBaseline.addNode(HCM_MODULE_YAML, NT_HCM_DESCRIPTOR).setProperty(HCM_YAML, "bla");
        session.save();

        final String actionList
                = "action-lists:\n"
                + "- 1.0:\n"
                + "    /content/path1: reload\n"
                + "    /content/pathX: reload\n"
                + "- 1.1:\n"
                + "    /content/path2: reload";
        actionListParser.parse(new ByteArrayInputStream(actionList.getBytes()), "String", module);

        assertEquals(null, module.getSequenceNumber());

        baselineService.updateModuleSequenceNumber(module);

        assertEquals(Double.valueOf(1.1), module.getSequenceNumber());
        assertTrue(moduleBaseline.getProperty(HCM_MODULE_SEQUENCE).getDouble() == 1.1);
    }
}
