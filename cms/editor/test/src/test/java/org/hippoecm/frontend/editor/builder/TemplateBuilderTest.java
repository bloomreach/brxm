/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.editor.repository.EditmodelWorkflow;
import org.hippoecm.editor.type.JcrTypeStore;
import org.hippoecm.frontend.EditorTestCase;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.impl.TemplateEngineFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.config.ClusterConfigEvent;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutSettings;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.TypeDescriptorEvent;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.Test;
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("serial")
public class TemplateBuilderTest extends EditorTestCase {

    private static class ExtPtModel implements IModel<Object> {

        public Object getObject() {
            return "${cluster.id}.field";
        }

        public void setObject(Object object) {
        }

        public void detach() {
        }
    }

    private static class PluginSelectModel  extends Model<String> {

    }

    @Test
    /**
     * verify that a plugin is added to the cluster when a field is added to the type
     */
    public void testAddField() throws Exception {
        IModel<String> pluginSelectModel = new PluginSelectModel();
        TemplateBuilder builder = new TemplateBuilder("test:edited", false, context, new ExtPtModel(), pluginSelectModel);

        final List<IPluginConfig> added = new LinkedList<>();
        final List<IPluginConfig> removed = new LinkedList<>();
        final List<IPluginConfig> changed = new LinkedList<>();
        final IClusterConfig config = builder.getTemplate();
        context.registerService(new IObserver<IObservable>() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return config;
            }

            public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                while (events.hasNext()) {
                    IEvent event = events.next();
                    if (event instanceof ClusterConfigEvent) {
                        ClusterConfigEvent cce = (ClusterConfigEvent) event;
                        IPluginConfig config = cce.getPlugin();
                        switch (cce.getType()) {
                        case PLUGIN_ADDED:
                            added.add(config);
                            break;
                        case PLUGIN_CHANGED:
                            changed.add(config);
                            break;
                        case PLUGIN_REMOVED:
                            removed.add(config);
                            break;
                        }
                    }
                }
            }

        }, IObserver.class.getName());

        BuiltinTypeStore builtinTypes = new BuiltinTypeStore();
        ITypeDescriptor type = builder.getTypeDescriptor();
        type.addField(new JavaFieldDescriptor("test", builtinTypes.load("nt:unstructured")));

        home.processEvents();

        assertEquals(1, added.size());
        IPluginConfig pluginConfig = added.get(0);
        assertEquals(pluginConfig.getName(), pluginSelectModel.getObject());
        assertEquals("${cluster.id}.field", pluginConfig.get("wicket.id"));
        assertEquals("nt_unstructured", pluginConfig.get("field"));
    }

    @Test
    /**
     * verify that a prototype is updated correctly when a mixin is added
     */
    public void testAddMixin() throws Exception {
        final WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        EditmodelWorkflow workflow = (EditmodelWorkflow) workflowManager.getWorkflow("default", session.getNode("/hippo:namespaces/test/document"));
        workflow.edit();

        IModel<String> pluginSelectModel = new PluginSelectModel();
        TemplateBuilder builder = new TemplateBuilder("test:document", false, context, new ExtPtModel(), pluginSelectModel);

        ITypeDescriptor type = builder.getTypeDescriptor();
        List<String> superTypes = new ArrayList<>(type.getSuperTypes());
        superTypes.add("test:mixin");
        type.setSuperTypes(superTypes);

        home.processEvents();

        final Node prototype = builder.getPrototype().getNode();
        Value[] mixinValues = prototype.getProperty(JcrConstants.JCR_MIXIN_TYPES).getValues();
        Set<String> mixins = new HashSet<>();
        for (Value value : mixinValues) {
            mixins.add(value.getString());
        }
        assertTrue(mixins.contains(JcrConstants.MIX_REFERENCEABLE));
        assertTrue(mixins.contains("test:mixin"));
    }

    @Test
    /**
     * verify that a field is removed from the type when a plugin is removed from the cluster
     */
    public void testRemovePlugin() throws Exception {
        TemplateBuilder builder = new TemplateBuilder("test:edited", false, context, new ExtPtModel(), new PluginSelectModel());

        // initialize type descriptor and template
        final ITypeDescriptor type = builder.getTypeDescriptor();
        IClusterConfig config = builder.getTemplate();
        List<IPluginConfig> plugins = config.getPlugins();
        session.save();
        home.processEvents();

        // setup field removal detection
        final List<String> added = new LinkedList<>();
        final List<String> removed = new LinkedList<>();
        context.registerService(new IObserver<IObservable>() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return type;
            }

            public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                while (events.hasNext()) {
                    IEvent event = events.next();
                    if (event instanceof TypeDescriptorEvent) {
                        TypeDescriptorEvent cce = (TypeDescriptorEvent) event;
                        String field = cce.getField().getName();
                        switch (cce.getType()) {
                        case FIELD_ADDED:
                            added.add(field);
                            break;
                        case FIELD_REMOVED:
                            removed.add(field);
                            break;
                        }
                    }
                }
            }

        }, IObserver.class.getName());

        // remove a field plugin
        List<IPluginConfig> newPlugins = new LinkedList<>(plugins);
        newPlugins.remove(1);
        config.setPlugins(newPlugins);

        home.processEvents();

        assertEquals(1, removed.size());
    }

    @Test
    public void testAddRemoveCycle() throws Exception {
        TemplateBuilder builder = new TemplateBuilder("test:edited", false, context, new ExtPtModel(), new PluginSelectModel());

        // initialize type descriptor and template
        final ITypeDescriptor type = builder.getTypeDescriptor();
        IClusterConfig config = builder.getTemplate();
        Node node = builder.getPrototype().getNode();

        BuiltinTypeStore typeStore = new BuiltinTypeStore();
        // add a field to the type: a plugin should be added
        List<IPluginConfig> plugins = config.getPlugins();
        type.addField(new JavaFieldDescriptor("test", typeStore.load("nt:unstructured")));
        home.processEvents();
        assertEquals(4, plugins.size());

        // add a child node to the prototype; it should be cleared up later
        node.addNode("test:nt_unstructured", "nt:unstructured");

        // remove the field plugin
        List<IPluginConfig> newPlugins = new LinkedList<>(plugins);
        newPlugins.remove(3);
        config.setPlugins(newPlugins);
        home.processEvents();

        // the field should be removed from the type, the child node should have
        // been removed from the prototype.
        assertEquals(2, type.getFields().size());
        assertFalse(node.hasNode("test:nt_unstructured"));
    }

    @Test
    public void testChangePath() throws Exception {
        PluginSelectModel pluginModel = new PluginSelectModel();
        pluginModel.setObject("title");

        TemplateBuilder builder = new TemplateBuilder("test:edited", false, context, new ExtPtModel(), pluginModel);

        // initialize type descriptor and template
        ITypeDescriptor type = builder.getTypeDescriptor();
        session.save();
        home.processEvents();

        Map<String, IFieldDescriptor> fields = type.getFields();
        IFieldDescriptor titleField = fields.get("title");
        assertEquals("test:title", titleField.getPath());

        JcrNodeModel prototype = builder.getPrototype();
        Node node = prototype.getNode();
        node.setProperty("test:title", "titel");

        titleField.setPath("test:titel_new");
        home.processEvents();

        // same plugin should still be selected
        assertEquals("titel_new", pluginModel.getObject());

        assertTrue(prototype.getNode().hasProperty("test:titel_new"));
        assertEquals("titel", prototype.getNode().getProperty("test:titel_new").getString());
        assertFalse(prototype.getNode().hasProperty("test:titel"));

        // remove field
        type.removeField("title");

        home.processEvents();

        assertFalse(prototype.getNode().hasProperty("test:titel_new"));
    }

    @Test
    public void testChangingMultiplicityUpdatesPrototype() throws Exception {
        PluginSelectModel pluginModel = new PluginSelectModel();
        pluginModel.setObject("title");

        TemplateBuilder builder = new TemplateBuilder("test:edited", false, context, new ExtPtModel(), pluginModel);

        // initialize type descriptor and template
        ITypeDescriptor type = builder.getTypeDescriptor();
        session.save();
        home.processEvents();

        Map<String, IFieldDescriptor> fields = type.getFields();
        IFieldDescriptor titleField = fields.get("title");
        assertEquals("test:title", titleField.getPath());

        JcrNodeModel prototype = builder.getPrototype();
        Node node = prototype.getNode();
        node.setProperty("test:title", "titel");

        titleField.setMultiple(true);
        home.processEvents();

        // same plugin should still be selected
        assertEquals("title", pluginModel.getObject());

        assertTrue(prototype.getNode().hasProperty("test:title"));

        Property property = prototype.getNode().getProperty("test:title");
        assertTrue(property.isMultiple());
        assertEquals("titel", property.getValues()[0].getString());

        // remove field
        type.removeField("title");

        home.processEvents();

        assertFalse(prototype.getNode().hasProperty("test:titel"));
    }

    @Test
    public void testSetPathOnNewTemplateChangesFieldName() throws Exception {
        TemplateBuilder builder = new TemplateBuilder("test:new", false, context, new ExtPtModel(), new PluginSelectModel());

        // initialize type descriptor and template
        ITypeDescriptor type = builder.getTypeDescriptor();
        session.save();
        home.processEvents();

        IFieldDescriptor newField = new JavaFieldDescriptor("test", new JcrTypeStore().load("String"));
        assertFalse("title".equals(newField.getName()));

        type.addField(newField);
        newField = type.getField(newField.getName());

        newField.setPath("test:title");
        assertEquals("title", newField.getName());

        IPluginConfig fieldPlugin = null;
        IClusterConfig cluster = builder.getTemplate();
        List<IPluginConfig> plugins = cluster.getPlugins();
        for (IPluginConfig plugin : plugins) {
            if (plugin.containsKey("field")) {
                if ("title".equals(plugin.getString("field"))) {
                    fieldPlugin = plugin;
                    break;
                }
            }
        }
        assertNotNull(fieldPlugin);
        assertEquals("title", fieldPlugin.getName());
    }

    @Test
    public void testSetPathOnSubTypePreservesFieldName() throws Exception {
        TemplateBuilder builder = new TemplateBuilder("test:edited", false, context, new ExtPtModel(), new PluginSelectModel());

        // initialize type descriptor and template
        ITypeDescriptor type = builder.getTypeDescriptor();
        session.save();
        home.processEvents();

        IFieldDescriptor newField = new JavaFieldDescriptor("test", new JcrTypeStore().load("String"));

        type.addField(newField);
        newField = type.getField(newField.getName());

        newField.setPath("test:extra");
        assertEquals("string", newField.getName());

        // plugin should have been renamed, but it's field should still be the same
        IPluginConfig fieldPlugin = null;
        IClusterConfig cluster = builder.getTemplate();
        List<IPluginConfig> plugins = cluster.getPlugins();
        for (IPluginConfig plugin : plugins) {
            if (plugin.containsKey("field")) {
                if ("string".equals(plugin.getString("field"))) {
                    fieldPlugin = plugin;
                    break;
                }
            }
        }
        assertNotNull(fieldPlugin);
        assertEquals("extra", fieldPlugin.getName());
    }

    @Test
    // regression test for HREPTWO-3155
    public void testRemoveLast() throws Exception {
        // YUCK!
        WebAppBehavior yuiWebApp = new WebAppBehavior(new WebAppSettings());
        home.add(yuiWebApp);
        home.add(new PageLayoutBehavior(new PageLayoutSettings()));

        Node templateTypeNode = session.getRootNode().getNode("hippo:namespaces/test/edited");
        JcrNodeModel nodeModel = new JcrNodeModel(templateTypeNode);
        ModelReference modelRef = new ModelReference("service.model", nodeModel);
        modelRef.init(context);

        TemplateEngineFactory factory = new TemplateEngineFactory(null);
        context.registerService(factory, "service.engine");
        ITemplateEngine engine = context.getService("service.engine", ITemplateEngine.class);

        ITypeDescriptor type = engine.getType(nodeModel);
        IClusterConfig template = engine.getTemplate(type, IEditor.Mode.EDIT);
        JavaPluginConfig parameters = new JavaPluginConfig();
        parameters.put("wicket.id", "service.root");
        parameters.put("wicket.model", "service.model");
        parameters.put("engine", "service.engine");
        parameters.put("mode", IEditor.Mode.EDIT.toString());
        IClusterControl cluster = context.newCluster(template, parameters);
        cluster.start();

        refreshPage();

        // remove
        tester.clickLink("root:extension.form:template:preview:view:1:item:head:remove");
        
        // remove
        tester.clickLink("root:extension.form:template:preview:view:1:item:head:remove");
    }

    @Test
    // regression test for HREPTWO-3155
    public void testMoveRemove() throws Exception {
        // YUCK!
        WebAppBehavior yuiWebApp = new WebAppBehavior(new WebAppSettings());
        home.add(yuiWebApp);
        home.add(new PageLayoutBehavior(new PageLayoutSettings()));

        Node templateTypeNode = session.getRootNode().getNode("hippo:namespaces/test/edited");
        JcrNodeModel nodeModel = new JcrNodeModel(templateTypeNode);
        ModelReference modelRef = new ModelReference("service.model", nodeModel);
        modelRef.init(context);

        TemplateEngineFactory factory = new TemplateEngineFactory(null);
        context.registerService(factory, "service.engine");
        ITemplateEngine engine = context.getService("service.engine", ITemplateEngine.class);

        ITypeDescriptor type = engine.getType(nodeModel);
        IClusterConfig template = engine.getTemplate(type, IEditor.Mode.EDIT);
        JavaPluginConfig parameters = new JavaPluginConfig();
        parameters.put("wicket.id", "service.root");
        parameters.put("wicket.model", "service.model");
        parameters.put("engine", "service.engine");
        parameters.put("mode", IEditor.Mode.EDIT.toString());
        IClusterControl cluster = context.newCluster(template, parameters);
        cluster.start();

        refreshPage();

        // down
        tester.clickLink("root:extension.form:template:preview:view:1:item:head:transitions:1:link");

        // remove
        tester.clickLink("root:extension.form:template:preview:view:1:item:head:remove");
    }

    @Test
    public void test_duplicated_name_is_updated_incrementally() throws Exception {
        final TemplateBuilder builder = new TemplateBuilder("test:new", false, context, new ExtPtModel(), new PluginSelectModel());

        // initialize type descriptor and template
        final ITypeDescriptor type = builder.getTypeDescriptor();
        session.save();
        home.processEvents();

        type.addField(new JavaFieldDescriptor("test", new JcrTypeStore().load("Boolean")));

        final IFieldDescriptor booleanField = type.getField("boolean");
        assertEquals("test:boolean", booleanField.getPath());

        IntStream.rangeClosed(1, 15).forEach(counter -> {
            try {
                type.addField(new JavaFieldDescriptor("test", new JcrTypeStore().load("Boolean")));
                final IFieldDescriptor duplicatedField = type.getField("boolean" + counter);
                assertEquals("test:boolean" + counter, duplicatedField.getPath());
            } catch (Exception e) {
                fail(String.format("Test is failed due to the exception -> %s", e.getMessage()));
            }
        });
    }
}
