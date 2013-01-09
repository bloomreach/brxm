/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.field;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.JavaTypeDescriptor;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class FieldPluginTest extends PluginTest {

    public interface TestService extends IClusterable {

        String getMode();
    }

    public static class TestPlugin extends RenderPlugin implements TestService {
        private static final long serialVersionUID = 1L;

        public TestPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, "service.test");
        }

        public String getMode() {
            return getPluginConfig().getString("mode");
        }

    }
    String[] testData = {
        "/test", "nt:unstructured",
        "/test/data", "nt:unstructured",
            "a", "noot",
        "/test/base", "nt:unstructured",
            "a", "aap",
        "/test/unmodified", "nt:unstructured",
            "a", "noot",
    };

    private JavaTypeDescriptor stringType;
    private JavaClusterConfig template;
    private JavaTypeDescriptor typeDesc;
    private JcrNodeModel model;
    private JavaFieldDescriptor fieldDesc;

    private IModelReference modelRef;
    private ITemplateEngine engine;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        build(session, testData);
        
        template = new JavaClusterConfig();
        template.addReference("wicket.model");
        template.addService("wicket.id");
        template.addProperty("mode");

        JavaPluginConfig plugin = new JavaPluginConfig("string-plugin");
        plugin.put("plugin.class", TestPlugin.class.getName());
        template.addPlugin(plugin);

        stringType = new JavaTypeDescriptor("String", "String", null);
        stringType.setIsNode(false);
        typeDesc = new JavaTypeDescriptor("type", "type", null);
        fieldDesc = new JavaFieldDescriptor("prefix", stringType);
        fieldDesc.setName("field");
        fieldDesc.setPath("a");
        typeDesc.addField(fieldDesc);
        model = new JcrNodeModel("/test/data");

        // set up services
        modelRef = createMock(IModelReference.class);
        context.registerService(modelRef, "service.model");

        engine = createMock(ITemplateEngine.class);
        context.registerService(engine, "service.engine");
    }

    private JavaPluginConfig createPluginConfig(String mode) {
        JavaPluginConfig config = new JavaPluginConfig("plugin");
        config.put("plugin.class", PropertyFieldPlugin.class.getName());
        config.put("wicket.id", "service.root");
        config.put("wicket.model", "service.model");
        config.put("engine", "service.engine");
        config.put("field", "field");
        if (mode != null) {
            config.put("mode", mode);
        }
        return config;
    }

    @Test
    public void defaultModeIsView() throws RepositoryException, TemplateEngineException {
        // set up expected service invocations
        expect(modelRef.getModel()).andReturn(model);
        expectLastCall().times(2);
        modelRef.setObservationContext((IObservationContext<? extends IObservable>) anyObject());
        modelRef.startObservation();
        expect(engine.getType(model)).andReturn(typeDesc);
        expect(engine.getTemplate(stringType, IEditor.Mode.VIEW)).andReturn(template);
        expectLastCall().times(2);

        replay(engine, modelRef);

        // play
        JavaPluginConfig config = createPluginConfig(null);
        start(config);
        tester.startPage(home);

        tester.assertLabel("root:name", "Field");
        assertNull(tester.getComponentFromLastRenderedPage("root:add"));
        assertNull(tester.getComponentFromLastRenderedPage("root:required"));

        List<TestService> services = context.getServices("service.test", TestService.class);
        assertEquals(1, services.size());
        assertEquals("view", services.get(0).getMode());

        // verify
        verify(engine, modelRef);        
    }

    @Test
    public void dontAutoCreateMultipleWhenNotRequired() throws RepositoryException, TemplateEngineException {
        fieldDesc.setMultiple(true);
        root.getNode("test/data").getProperty("a").remove();
        root.save();

        // set up expected service invocations
        expect(modelRef.getModel()).andReturn(model);
        expectLastCall().times(2);
        modelRef.setObservationContext((IObservationContext<? extends IObservable>) anyObject());
        modelRef.startObservation();
        expect(engine.getType(model)).andReturn(typeDesc);
        expect(engine.getTemplate(stringType, IEditor.Mode.EDIT)).andReturn(template);
        expectLastCall().times(1);

        replay(engine, modelRef);

        // play
        JavaPluginConfig config = createPluginConfig("edit");
        start(config);
        tester.startPage(home);

        assertFalse(session.itemExists("/test/data/a"));

        List<TestService> services = context.getServices("service.test", TestService.class);
        assertEquals(0, services.size());

        // verify
        verify(engine, modelRef);        
    }

    @Test
    public void compareOpensTwoInstancesWhenModeIsNotSupported() throws RepositoryException, TemplateEngineException {
        // set up expected service invocations
        expect(modelRef.getModel()).andReturn(model);
        expectLastCall().times(2);
        modelRef.setObservationContext((IObservationContext<? extends IObservable>) anyObject());
        modelRef.startObservation();
        expect(engine.getType(model)).andReturn(typeDesc);
        expect(engine.getTemplate(stringType, IEditor.Mode.COMPARE)).andReturn(template);
        expect(engine.getTemplate(stringType, IEditor.Mode.VIEW)).andReturn(template);
        expectLastCall().times(2);

        JcrNodeModel baseModel = new JcrNodeModel("/test/base");
        IModelReference baseRef = createMock(IModelReference.class);
        expect(baseRef.getModel()).andReturn(baseModel);
        context.registerService(baseRef, "service.base");

        replay(engine, modelRef, baseRef);

        // play
        JavaPluginConfig config = createPluginConfig("compare");
        config.put("model.compareTo", "service.base");
        start(config);
        tester.startPage(home);

        tester.assertLabel("root:name", "Field");
        assertNull(tester.getComponentFromLastRenderedPage("root:add"));
        assertNull(tester.getComponentFromLastRenderedPage("root:required"));

        List<TestService> services = context.getServices("service.test", TestService.class);
        assertEquals(2, services.size());
        assertEquals("view", services.get(0).getMode());
        assertEquals("view", services.get(1).getMode());

        // verify
        verify(engine, modelRef, baseRef);        
    }

    @Test
    public void compareViewIsOpenedWhenModeIsSupported() throws RepositoryException, TemplateEngineException {
        template.addReference("model.compareTo");

        // set up expected service invocations
        expect(modelRef.getModel()).andReturn(model);
        expectLastCall().times(2);
        modelRef.setObservationContext((IObservationContext<? extends IObservable>) anyObject());
        modelRef.startObservation();
        expect(engine.getType(model)).andReturn(typeDesc);
        expect(engine.getTemplate(stringType, IEditor.Mode.COMPARE)).andReturn(template);

        JcrNodeModel baseModel = new JcrNodeModel("/test/base");
        IModelReference baseRef = createMock(IModelReference.class);
        expect(baseRef.getModel()).andReturn(baseModel);
        context.registerService(baseRef, "service.base");

        replay(engine, modelRef, baseRef);

        // play
        JavaPluginConfig config = createPluginConfig("compare");
        config.put("model.compareTo", "service.base");
        start(config);
        tester.startPage(home);

        tester.assertLabel("root:name", "Field");
        assertNull(tester.getComponentFromLastRenderedPage("root:add"));
        assertNull(tester.getComponentFromLastRenderedPage("root:required"));

        List<TestService> services = context.getServices("service.test", TestService.class);
        assertEquals(1, services.size());
        assertEquals("compare", services.get(0).getMode());

        // verify
        verify(engine, modelRef, baseRef);        
    }

    @Test
    public void compareOpensViewWhenModelHasNotChanged() throws RepositoryException, TemplateEngineException {
        // set up expected service invocations
        expect(modelRef.getModel()).andReturn(model);
        expectLastCall().times(2);
        modelRef.setObservationContext((IObservationContext<? extends IObservable>) anyObject());
        modelRef.startObservation();
        expect(engine.getType(model)).andReturn(typeDesc);
//        expect(engine.getTemplate(stringType, IEditor.Mode.COMPARE)).andReturn(template);
        expect(engine.getTemplate(stringType, IEditor.Mode.VIEW)).andReturn(template);

        JcrNodeModel baseModel = new JcrNodeModel("/test/unmodified");
        IModelReference baseRef = createMock(IModelReference.class);
        expect(baseRef.getModel()).andReturn(baseModel);
        context.registerService(baseRef, "service.base");

        replay(engine, modelRef, baseRef);

        // play
        JavaPluginConfig config = createPluginConfig("compare");
        config.put("model.compareTo", "service.base");
        start(config);
        tester.startPage(home);

        tester.assertLabel("root:name", "Field");
        assertNull(tester.getComponentFromLastRenderedPage("root:add"));
        assertNull(tester.getComponentFromLastRenderedPage("root:required"));

        List<TestService> services = context.getServices("service.test", TestService.class);
        assertEquals(1, services.size());
        assertEquals("view", services.get(0).getMode());

        // verify
        verify(engine, modelRef, baseRef);        
    }

}
