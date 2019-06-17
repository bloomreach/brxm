/*
 *  Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.validator;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.jcr.Node;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.editor.editor.EditorPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidationPluginTest extends PluginTest {

    final static String[] content = {
            "/test", "nt:unstructured",

            "/test/plugin", "frontend:plugin",
                "plugin.class", EditorPlugin.class.getName(),
                "wicket.model", "service.model",
                "validator.id", "service.validator",
                "editor.id", "${cluster.id}.editor",

            "/config/test-app/validator", "frontend:plugincluster",

            "/config/test-app/validator/registry", "frontend:plugin",
                "plugin.class", ValidatorService.class.getName(),
                ValidatorService.FIELD_VALIDATOR_SERVICE_ID, ValidatorService.DEFAULT_FIELD_VALIDATOR_SERVICE,
    };

    IPluginConfig config;
    IPluginConfig validator;
    IPluginConfig registry;

    private ModelReference<Node> modelRef;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);

        final JcrNodeModel nodeModel = new JcrNodeModel("/test/content");
        modelRef = new ModelReference<>("service.model", nodeModel);
        modelRef.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
        validator = new JcrClusterConfig(new JcrNodeModel("/config/test-app/validator"));
        registry = new JcrClusterConfig(new JcrNodeModel("/config/test-app/validator/registry"));
    }

    protected Set<Violation> getViolations() {
        return context.getService("service.validator", IValidationService.class).getValidationResult().getViolations();
    }

    protected void validate(final Node node) throws ValidationException {
        modelRef.setModel(new JcrNodeModel(node));
        context.getService("service.validator", IValidationService.class).validate();
    }

    protected void detach() {
        ((IDetachable) context.getService("service.validator", IValidationService.class)).detach();
    }

    @Test
    public void testRequiredProperty() throws Exception {
        start(config);
        start(validator);
        start(registry);

        final Node content = root.getNode("test").addNode("content", "test:validator");
        validate(content);

        final Set<Violation> violations = getViolations();
        assertEquals(2, violations.size());
    }

    @Test
    public void testRequiredChildNode() throws Exception {
        start(config);
        start(validator);
        start(registry);

        final Node content = root.getNode("test").addNode("content", "test:container");
        validate(content);

        final Set<Violation> violations = getViolations();
        assertEquals(2, violations.size());

        final Set<String> jcrPaths = getJcrPaths(violations);
        assertTrue(jcrPaths.contains("test:single"));
        assertTrue(jcrPaths.contains("test:multiple"));
    }

    private Set<String> getJcrPaths(final Set<Violation> violations) {
        final Set<String> jcrPaths = new TreeSet<>();
        for (final Violation violation : violations) {
            final Set<ModelPath> paths = violation.getDependentPaths();
            assertEquals(1, paths.size());

            final ModelPathElement[] elements = paths.iterator().next().getElements();
            assertEquals(2, elements.length);
            jcrPaths.add(elements[1].getField().getPath());
        }
        return jcrPaths;
    }

    @Test
    public void testPathTranslation() throws Exception {
        start(config);
        start(validator);
        start(registry);

        final Node content = root.getNode("test").addNode("content", "test:container");
        content.addNode("test:single", "test:validator");
        content.addNode("test:multiple", "test:validator");
        validate(content);

        final Set<Violation> violations = getViolations();
        assertEquals(4, violations.size());

        final Set<String> jcrPaths = new TreeSet<>();
        for (final Violation violation : violations) {
            final Set<ModelPath> paths = violation.getDependentPaths();
            assertEquals(1, paths.size());

            final ModelPathElement[] elements = paths.iterator().next().getElements();
            jcrPaths.add(Arrays.stream(elements).map(
                    element -> element.getName() + "[" + (element.getIndex() + 1) + "]").collect(
                    Collectors.joining("/")));
            assertEquals(3, elements.length);
        }

        assertTrue(jcrPaths.contains("content[1]/test:single[1]/test:mandatory[1]"));
        assertTrue(jcrPaths.contains("content[1]/test:single[1]/test:multiple[1]"));
        assertTrue(jcrPaths.contains("content[1]/test:multiple[1]/test:mandatory[1]"));
        assertTrue(jcrPaths.contains("content[1]/test:multiple[1]/test:multiple[1]"));
    }

    @Test
    public void testCascading() throws Exception {
        start(config);
        start(validator);
        start(registry);

        final Node content = root.getNode("test").addNode("content", "test:container");
        final Node uncascaded = content.addNode("test:uncascaded", "test:uncascaded");
        uncascaded.setProperty("test:property", "");
        validate(content);

        final Set<Violation> violations = getViolations();
        assertEquals(2, violations.size());
    }
}
