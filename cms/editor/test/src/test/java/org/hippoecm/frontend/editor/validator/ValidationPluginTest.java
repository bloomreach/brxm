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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

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
                "field.validator.service.id", "field.validator.service",
    };

    IPluginConfig config;
    IPluginConfig validator;
    IPluginConfig registry;

    private ModelReference modelRef;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);

        JcrNodeModel nodeModel = new JcrNodeModel("/test/content");
        modelRef = new ModelReference("service.model", nodeModel);
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

        Node content = root.getNode("test").addNode("content", "test:validator");
        validate(content);

        Set<Violation> violations = getViolations();
        assertEquals(2, violations.size());
    }

    @Test
    public void testRequiredChildNode() throws Exception {
        start(config);

        Node content = root.getNode("test").addNode("content", "test:container");
        validate(content);

        Set<Violation> violations = getViolations();
        assertEquals(2, violations.size());

        Set<String> jcrPaths = getJcrPaths(violations);
        assertTrue(jcrPaths.contains("test:single"));
        assertTrue(jcrPaths.contains("test:multiple"));
    }

    private Set<String> getJcrPaths(Set<Violation> violations) {
        Set<String> jcrPaths = new TreeSet<String>();
        Iterator<Violation> iter = violations.iterator();
        while (iter.hasNext()) {
            Violation violation = iter.next();
            Set<ModelPath> paths = violation.getDependentPaths();
            assertEquals(1, paths.size());

            ModelPathElement[] elements = paths.iterator().next().getElements();
            assertEquals(1, elements.length);
            jcrPaths.add(elements[0].getField().getPath());
        }
        return jcrPaths;
    }

    @Test
    public void testPathTranslation() throws Exception {
        start(config);

        Node content = root.getNode("test").addNode("content", "test:container");
        content.addNode("test:single", "test:validator");
        content.addNode("test:multiple", "test:validator");
        validate(content);

        Set<Violation> violations = getViolations();
        assertEquals(4, violations.size());

        Set<String> jcrPaths = new TreeSet<String>();
        Iterator<Violation> iter = violations.iterator();
        while (iter.hasNext()) {
            Violation violation = iter.next();
            Set<ModelPath> paths = violation.getDependentPaths();
            assertEquals(1, paths.size());

            ModelPathElement[] elements = paths.iterator().next().getElements();
            assertEquals(2, elements.length);
            jcrPaths.add(elements[0].getName() + '[' + (elements[0].getIndex() + 1) + ']' + '/' + elements[1].getName()
                    + '[' + (elements[1].getIndex() + 1) + ']');
        }

        assertTrue(jcrPaths.contains("test:single[1]/test:mandatory[1]"));
        assertTrue(jcrPaths.contains("test:single[1]/test:multiple[1]"));
        assertTrue(jcrPaths.contains("test:multiple[1]/test:mandatory[1]"));
        assertTrue(jcrPaths.contains("test:multiple[1]/test:multiple[1]"));
    }

    @Test
    public void testCascading() throws Exception {
        start(config);

        Node content = root.getNode("test").addNode("content", "test:container");
        Node uncascaded = content.addNode("test:uncascaded", "test:uncascaded");
        uncascaded.setProperty("test:property", "");
        validate(content);

        Set<Violation> violations = getViolations();
        assertEquals(2, violations.size());
    }

    /*
    @Test
    public void testSetPathFailsWhenSubtypeHasSamePath() throws Exception {
        JcrNodeModel nodeModel = new JcrNodeModel("/hippo:namespaces/test/test/hippo:nodetype/hippo:nodetype[2]");
        JcrTypeDescriptor typeDescriptor = new JcrTypeDescriptor(nodeModel, new JcrTypeLocator());
        typeDescriptor.getField("title").setPath("test:extra");

        TemplateTypeValidator validator = new TemplateTypeValidator();
        Set<Violation> violations = validator.validate(nodeModel);
        assertEquals(1, violations.size());
    }
    */

}
