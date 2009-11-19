/*
 *  Copyright 2009 Hippo.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.IValidateService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.junit.Before;
import org.junit.Test;

public class ValidationPluginTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static String[] content = {
        "/test", "nt:unstructured",
            "/test/plugin", "frontend:plugin",
                "plugin.class", ValidationPlugin.class.getName(),
                "wicket.model", "service.model",
                "validator.id", "service.validator",
                "validator.model", "model.validator",
    };
    IPluginConfig config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        build(session, content);

        JcrNodeModel nodeModel = new JcrNodeModel("/test/content");
        ModelReference modelRef = new ModelReference("service.model", nodeModel);
        modelRef.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    protected Set<Violation> getViolations() {
        IModelReference validationRef = context.getService("model.validator", IModelReference.class);
        return ((IValidationResult) validationRef.getModel().getObject()).getViolations();
    }

    protected void validate(final Node node) throws ValidationException {
        context.getService("service.validator", IValidateService.class).validate();
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

}
