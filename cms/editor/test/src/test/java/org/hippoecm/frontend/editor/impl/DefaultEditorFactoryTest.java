/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.impl;

import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.editor.IEditorContext;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DefaultEditorFactoryTest extends PluginTest {

    static final String EDITORS = "editors";
    static final String PREVIEWS = "previews";
    static final String COMPARERS = "compares";
    static final String FILTERS = "filters";
    static final String RENDERERS = "service.renderer";
    
    public static class Editor extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public Editor(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, EDITORS);
        }
    }

    public static class Preview extends RenderPlugin implements ITitleDecorator {
        private static final long serialVersionUID = 1L;

        public Preview(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, PREVIEWS);
        }

        public IModel getTitle() {
            try {
                return new Model(((JcrNodeModel) getModel()).getNode().getName());
            } catch (RepositoryException ex) {
                throw new RuntimeException("failed to determine node name", ex);
            }
        }

        public ResourceReference getIcon(IconSize type) {
            return null;
        }

    }

    public static class Comparer extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public Comparer(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, COMPARERS);
        }

        public IModel getCompareToModel() {
            return getPluginContext().getService(getPluginConfig().getString("model.compareTo"), IModelReference.class)
                    .getModel();
        }
    }

    public static class CloseFilter extends Plugin implements IEditorFilter {
        private static final long serialVersionUID = 1L;

        boolean closed = false;

        public CloseFilter(IPluginContext context, IPluginConfig config) {
            super(context, config);

            IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
            context.registerService(this, context.getReference(editor).getServiceId());

            context.registerService(this, FILTERS);
        }

        public Object preClose() {
            closed = true;
            return new Object();
        }

        public void postClose(Object object) {
        }
    }

    private List<IRenderService> getPreviews() {
        return context.getServices(PREVIEWS, IRenderService.class);
    }

    private List<IRenderService> getEditors() {
        return context.getServices(EDITORS, IRenderService.class);
    }

    private List<IRenderService> getComparers() {
        return context.getServices(COMPARERS, IRenderService.class);
    }

    private List<CloseFilter> getCloseFilters() {
        return context.getServices(FILTERS, CloseFilter.class);
    }

    private final class TestEditorContext implements IEditorContext {
        public IEditorManager getEditorManager() {
            // TODO Auto-generated method stub
            return null;
        }

        public void onClose() {
            // TODO Auto-generated method stub
            
        }

        public void onFocus() {
            // TODO Auto-generated method stub
            
        }
    }

    final static String[] content1 = {
            "/test", "nt:unstructured",
                "/test/content", "nt:unstructured",
                    "jcr:mixinTypes", "mix:referenceable"
    };
    final static String[] content2 = {
                "/test/mirror", "hippo:mirror",
                    "hippo:docbase", "/test/content",
            "/config/test-app/cms-editor", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:services", "wicket.id",
                "/config/test-app/cms-editor/plugin", "frontend:plugin",
                    "plugin.class", Editor.class.getName(),
            "/config/test-app/cms-preview", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:references", "editor.id",
                "frontend:services", "wicket.id",
                "/config/test-app/cms-preview/plugin", "frontend:plugin",
                    "plugin.class", Preview.class.getName(),
                "/config/test-app/cms-preview/filter", "frontend:plugin",
                    "plugin.class", CloseFilter.class.getName(),
            "/config/test-app/cms-compare", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:references", "model.compareTo",
                "frontend:services", "wicket.id",
                "/config/test-app/cms-compare/plugin", "frontend:plugin",
                    "plugin.class", Comparer.class.getName(),
    };

    final static String[] cmstestdocument = new String[] {
            "/${name}", "hippo:handle",
                "jcr:mixinTypes", "mix:referenceable",
                "/${name}/${name}", "cmstest:document",
                    "jcr:mixinTypes", "mix:referenceable"
            
    };

    final static String[] plaintestdocument = new String[] {
            "/${name}", "hippo:handle",
                "jcr:mixinTypes", "mix:referenceable",
                "/${name}/${name}", "hippo:document",
                    "jcr:mixinTypes", "mix:referenceable",
            
    };

    static IPluginConfig parameters;
    static {
        parameters = new JavaPluginConfig();
        parameters.put("wicket.id", RENDERERS);
    }
    
    IPluginConfig config;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();

        build(session, content1);
        session.save();
        build(session, content2);
        session.save();

        config = new JavaPluginConfig("plugin");
        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        try {
            String uri = nsReg.getURI("content");
            assertEquals("http://content/1.0", uri);
        } catch (NamespaceException e) {
            nsReg.registerNamespace("content", "http://content/1.0");
        }
    }

    protected void createDocument(String name) throws RepositoryException {
        Map<String, String> pars = new MiniMap(1);
        pars.put("name", name);
        build(session, mount("/test/content", instantiate(cmstestdocument, pars)));
    }

    @Test
    public void testTemplateType() throws Exception {
        Node test = root.getNode("test/content").addNode("template", HippoNodeType.NT_TEMPLATETYPE);
        test.addNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE, HippoNodeType.NT_HANDLE);
        session.save();

        // open preview
        DefaultEditorFactoryPlugin factory = new DefaultEditorFactoryPlugin(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/template"),
                Mode.VIEW, parameters);

        assertEquals(1, getPreviews().size());
        assertEquals(0, getEditors().size());

        // edit type
        editor.setMode(IEditor.Mode.EDIT);
        assertEquals(0, getPreviews().size());
        assertEquals(1, getEditors().size());

        // done editing
        editor.setMode(IEditor.Mode.VIEW);
        assertEquals(1, getPreviews().size());
        assertEquals(0, getEditors().size());
    }

    @Test
    public void testVersionedDocumentCanOnlyBeOpenedInCompareMode() throws Exception {
        Map<String, String> pars = new MiniMap<String, String>(1);
        pars.put("name", "document");
        build(session, mount("/test/content", instantiate(plaintestdocument, pars)));

        session.save();

        Version version = root.getNode("test/content/document/document").checkin();

        DefaultEditorFactoryPlugin factory = new DefaultEditorFactoryPlugin(context, config);
        try {
            factory.newEditor(new TestEditorContext(), new JcrNodeModel(version), Mode.EDIT, parameters);
            fail("Could open Version in edit mode");
        } catch (EditorException e) {
            // ignore, this is OK
        }
        try {
            factory.newEditor(new TestEditorContext(), new JcrNodeModel(version), Mode.VIEW, parameters);
            fail("Could open Version in view mode");
        } catch (EditorException e) {
            // ignore, this is OK
        }
        factory.newEditor(new TestEditorContext(), new JcrNodeModel(version), Mode.COMPARE, parameters);
    }

    @Test
    public void testVersionedDocument() throws Exception {
        Map<String, String> pars = new MiniMap<String, String>(1);
        pars.put("name", "document");
        build(session, mount("/test/content", instantiate(plaintestdocument, pars)));

        session.save();

        Version version = root.getNode("test/content/document/document").checkin();

        DefaultEditorFactoryPlugin factory = new DefaultEditorFactoryPlugin(context, config);
        IEditor editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel(version), Mode.COMPARE, parameters);
        List<IRenderService> comparers = getComparers();
        assertEquals(1, comparers.size());
        Comparer comparer = (Comparer) comparers.get(0);
        JcrNodeModel current = (JcrNodeModel) comparer.getModel();
        JcrNodeModel base = (JcrNodeModel) comparer.getCompareToModel();
        assertEquals(new JcrNodeModel("/test/content/document/document"), current);
        assertEquals(new JcrNodeModel(version.getNode("jcr:frozenNode")), base);
    }

    @Test // Apparently the editor is not closed for non-publishable documents
    public void deleteDocumentClosesEditor() throws Exception {
        createDocument("document");

        // open preview
        DefaultEditorFactoryPlugin factory = new DefaultEditorFactoryPlugin(context, config);
        JcrNodeModel model = new JcrNodeModel("/test/content/document");
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), model, Mode.VIEW, parameters);

        assertEquals(1, getPreviews().size());

        session.getRootNode().getNode("test/content").remove();

        ((IDetachable) editor).detach();
        ((IRefreshable) editor).refresh();

        assertEquals(0, getPreviews().size());
    }
}
