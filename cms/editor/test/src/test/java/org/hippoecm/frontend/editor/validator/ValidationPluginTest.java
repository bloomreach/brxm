/*
 *  Copyright 2011 Hippo.
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

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.editor.validator.plugins.EscapedValidatorPlugin;
import org.hippoecm.frontend.i18n.ConfigTraversingPlugin;
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

import javax.jcr.Node;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidationPluginTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";


    final static String[] content = {
            "/test", "nt:unstructured",
            "/test/plugin", "frontend:plugin",
            "plugin.class", ValidationPlugin.class.getName(),
            "wicket.model", "service.model",
            "validator.id", "service.validator",
            "/config/test-app/validator", "frontend:plugincluster",
            "translator.id", "${cluster.id}.translator",
            "/config/test-app/validator/registry", "frontend:plugin",
            "plugin.class", AdvancedValidatorService.class.getName(),
            "advanced.validator.service.id", "advanced.validator.service",
            "/config/test-app/validator/escaped", "frontend:plugin",
            "plugin.class", EscapedValidatorPlugin.class.getName(),

            "/config/test-app/validator/translation", "frontend:plugin",
            "jcr:mixinTypes", "hippostd:translated",
            "plugin.class", ConfigTraversingPlugin.class.getName(),

            "/config/test-app/validator/translation/hippostd:translations", "hippostd:translations",

            "/config/test-app/validator/translation/hippostd:translations/escaped", "frontend:pluginconfig",
            "jcr:mixinTypes", "hippostd:translated",

            /*"/config/test-app/validator/translation/hippostd:translations/escaped/hippo:translation", "hippo:translation",
            "hippo:language", "en",
            "hippo:message", "escaped exceptie",*/

    };
    IPluginConfig config;
    IPluginConfig validator;
    IPluginConfig registry;
    IPluginConfig escaped;
    IPluginConfig translation;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);

        JcrNodeModel nodeModel = new JcrNodeModel("/test/content");
        ModelReference modelRef = new ModelReference("service.model", nodeModel);
        modelRef.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
        validator = new JcrClusterConfig(new JcrNodeModel("/config/test-app/validator"));
        registry = new JcrClusterConfig(new JcrNodeModel("/config/test-app/validator/registry"));
        escaped = new JcrClusterConfig(new JcrNodeModel("/config/test-app/validator/escaped"));
        translation = new JcrClusterConfig(new JcrNodeModel("/config/test-app/validator/translation"));
    }


    protected Set<Violation> getViolations() {
        return context.getService("service.validator", IValidationService.class).getValidationResult().getViolations();
    }

    protected void validate(final Node node) throws ValidationException {
        context.getService("service.validator", IValidationService.class).validate();
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
    public void testEscapedProperty() throws Exception {
        start(config);
        start(validator);
        start(registry);
        start(escaped);
        start(translation);

        Node content = root.getNode("test").addNode("content", "test:validator");
        content.setProperty("test:nonempty", "something");
        content.setProperty("test:mandatory", "something");
        content.setProperty("test:multiple", new String[]{"something"});
        validate(content);

        Set<Violation> violations = getViolations();
        assertEquals(0, violations.size());

        for (char c : "<>\"'&".toCharArray()) {
            content.setProperty("test:escaped", "a" + c);
            validate(content);
            violations = getViolations();
            assertEquals(1, violations.size());
        }
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
