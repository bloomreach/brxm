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
package org.hippoecm.frontend.plugins.cms.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.reviewedactions.PublishableDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EditorManagerTest extends PluginTest implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final String EDITORS = "editors";
    static final String PREVIEWS = "previews";
    static final String FILTERS = "filters";
    static final String RENDERERS = "editor.render";

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

    protected List<IRenderService> getPreviews() {
        return context.getServices(PREVIEWS, IRenderService.class);
    }

    protected List<IRenderService> getEditors() {
        return context.getServices(EDITORS, IRenderService.class);
    }

    protected List<CloseFilter> getCloseFilters() {
        return context.getServices(FILTERS, CloseFilter.class);
    }

    protected List<IRenderService> getRenderers() {
        return context.getServices(RENDERERS, IRenderService.class);
    }

    final static String[] content1 = {
            "/test", "nt:unstructured",
                "/test/content", "nt:unstructured",
                    "jcr:mixinTypes", "mix:referenceable"
    };
    final static String[] content2 = {
                "/test/facetsearch", "hippo:facetsearch",
                    "hippo:docbase", "/test/content",
                    "hippo:queryname", "state",
                    "hippo:facets", HippoStdNodeType.HIPPOSTD_STATE,
                "/test/plugin", "frontend:pluginconfig",
                    "plugin.class", EditorManagerPlugin.class.getName(),
                    "wicket.model", "model",
                    "editor.id", "editor.manager",
                    "cluster.edit.name", "editor",
                    "cluster.preview.name", "preview",
                    "/test/plugin/cluster.edit.options", "frontend:pluginconfig",
                        "wicket.id", RENDERERS,
                    "/test/plugin/cluster.preview.options", "frontend:pluginconfig",
                        "wicket.id", RENDERERS,
            "/config/test-app/editor", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:services", "wicket.id",
                "/config/test-app/editor/plugin", "frontend:plugin",
                    "plugin.class", Editor.class.getName(),
                    "wicket.id", "${wicket.id}",
            "/config/test-app/preview", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:references", "editor.id",
                "frontend:services", "wicket.id",
                "/config/test-app/preview/plugin", "frontend:plugin",
                    "plugin.class", Preview.class.getName(),
                    "wicket.id", "${wicket.id}",
                    "wicket.model", "${wicket.model}",
                "/config/test-app/preview/filter", "frontend:plugin",
                    "plugin.class", CloseFilter.class.getName(),
                    "editor.id", "${editor.id}"
    };

    final static String[] testdocument = new String[] {
            "/${name}", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
                "/${name}/${name}", "cmstest:document",
                    "jcr:mixinTypes", "hippo:harddocument",
                    HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.UNPUBLISHED,
                    HippoStdNodeType.HIPPOSTD_STATESUMMARY, HippoStdNodeType.NEW,
                    "hippostdpubwf:createdBy", "admin",
                    "hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00",
                    "hippostdpubwf:lastModifiedBy", "admin",
                    "hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00"
            
    };

    ModelReference modelReference;
    IPluginConfig config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content1);
        session.save();
        build(session, content2);
        session.save();

        modelReference = new ModelReference("model", new JcrNodeModel((Node) null));
        modelReference.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected void createDocument(String name) throws RepositoryException {
        Map<String, String> pars = new MiniMap(1);
        pars.put("name", name);
        build(session, mount("/test/content", instantiate(testdocument, pars)));
    }

    @Test
    public void testOpenPreviewOnBrowse() {
        IPluginContext pluginContext = start(config);
        modelReference.setModel(new JcrNodeModel("/test/content"));
        // default editor uses edit cluster
        assertEquals(1, getPreviews().size());
    }

    @Test
    public void testReviewedActionsEditing() throws Exception {
        createDocument("document");

        IPluginContext pluginContext = start(config);
        modelReference.setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(1, getPreviews().size());

        // simulate workflow step "obtainEditableInstance"
        Node unpublished = session.getRootNode().getNode("test/content/document/document");
        Node draft = ((HippoSession) session).copy(unpublished, unpublished.getPath());
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "draft");
        draft.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, CREDENTIALS.getString("username"));
        session.save();
        home.processEvents();
        assertEquals(1, getEditors().size());
    }

    @Test
    public void testEditPublished() throws Exception {
        createDocument("document");

        Node unpublished = session.getRootNode().getNode("test/content/document/document");
        unpublished.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        session.save();

        IPluginContext pluginContext = start(config);
        modelReference.setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(1, getPreviews().size());

        // simulate workflow step "obtainEditableInstance"
        Node draft = ((HippoSession) session).copy(unpublished, unpublished.getPath());
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "draft");
        draft.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, CREDENTIALS.getString("username"));
        session.save();
        home.processEvents();
        assertEquals(1, getEditors().size());

        // simulate "commitEditableInstance"
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "unpublished");
        session.save();
        home.processEvents();
        assertEquals(1, getPreviews().size());
        assertEquals(0, getEditors().size());
    }

    @Test
    public void testSetMode() throws Exception {

        String[] workflowConfig = {
                "/publishable", "hipposys:workflow",
                    "hipposys:nodetype", "hippostd:publishable",
                    "hipposys:classname", org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflowImpl.class.getName(),
                    "hipposys:display", "publishable workflow",
                    // "/publishable/hipposys:types", "hipposys:types", // autocreated
                        "/publishable/hipposys:types/" + PublishableDocument.class.getName(), "hipposys:type",
                            "hipposys:classname", PublishableDocument.class.getName(),
                            "hipposys:display", "publisahble document",
                            "hipposys:nodetype", "hippostd:publishable"
        };
        build(session, mount("/hippo:configuration/hippo:workflows/default", workflowConfig));
        Node category = session.getRootNode().getNode("hippo:configuration/hippo:workflows/default");
        category.orderBefore("publishable", category.getNodes().nextNode().getName());

        createDocument("document");

        session.save();

        start(config);
        IEditorManager editorMgr = context.getService("editor.manager", IEditorManager.class);
        IEditor editor = editorMgr.openEditor(new JcrNodeModel("/test/content/document"));
        assertEquals(1, getPreviews().size());
        assertEquals(0, getEditors().size());

        editor.setMode(IEditor.Mode.EDIT);
        assertEquals(0, getPreviews().size());
        assertEquals(1, getEditors().size());
    }
    
    @Test
    public void browserFollowsActiveEditor() throws Exception {
        createDocument("doc1");
        createDocument("doc2");

        IPluginContext pluginContext = start(config);

        // open two editors
        modelReference.setModel(new JcrNodeModel("/test/content/doc1"));
        assertEquals(1, getPreviews().size());

        modelReference.setModel(new JcrNodeModel("/test/content/doc2"));
        assertEquals(2, getPreviews().size());
        assertEquals(new JcrNodeModel("/test/content/doc2"), modelReference.getModel());

        // switch focus
        List<IRenderService> previews = getPreviews();
        previews.get(0).focus(null);
        assertEquals(new JcrNodeModel("/test/content/doc1"), modelReference.getModel());
    }

    @Test
    public void browseToNullOnEditorClose() throws Exception {
        createDocument("document");
        start(config);

        // open preview
        JcrNodeModel model = new JcrNodeModel("/test/content/document");
        IEditorManager editorMgr = context.getService("editor.manager", IEditorManager.class);
        IEditor editor = editorMgr.openEditor(model);
        assertEquals(model, modelReference.getModel());

        // close editor
        editor.close();
        assertEquals(new JcrNodeModel((Node) null), modelReference.getModel());
    }

    @Test
    public void invokeCloseFilter() throws Exception {
        createDocument("document");

        IPluginContext pluginContext = start(config);

        // open editor
        modelReference.setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(1, getCloseFilters().size());
        List<CloseFilter> filters = getCloseFilters();

        // close editor
        IRenderService renderer = getRenderers().get(0);
        IEditor editor = context.getService(context.getReference(renderer).getServiceId(), IEditor.class);
        editor.close();
        assertTrue(filters.get(0).closed);
    }

    @Test
    public void previewPhysicalNode() throws Exception {
        createDocument("document");
        session.save();

        IPluginContext pluginContext = start(config);

        // open editor
        modelReference.setModel(new JcrNodeModel("/test/facetsearch/unpublished/hippo:resultset/document"));
        assertEquals(1, getPreviews().size());
        Preview preview = (Preview) getPreviews().get(0);
        assertEquals(new JcrNodeModel("/test/content/document/document"), preview.getModel());
    }

    @Test
    public void browseToVirtualNode() throws Exception {
        createDocument("doc1");
        createDocument("doc2");
        session.save();

        IPluginContext pluginContext = start(config);

        // open editor for virtual node
        modelReference.setModel(new JcrNodeModel("/test/facetsearch/unpublished/hippo:resultset/doc1"));
        assertEquals(1, getPreviews().size());

        // open editor for physical node
        modelReference.setModel(new JcrNodeModel("/test/content/doc2/doc2"));
        assertEquals(2, getPreviews().size());

        // switch back to first editor
        List<IRenderService> previews = getPreviews();
        previews.get(0).focus(null);
        assertEquals(new JcrNodeModel("/test/facetsearch/unpublished/hippo:resultset/doc1"), modelReference.getModel());
    }

    @Test
    public void testTemplateType() throws Exception {
        Node test = root.getNode("test/content").addNode("template", HippoNodeType.NT_TEMPLATETYPE);
        test.addNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE, HippoNodeType.NT_HANDLE);
        session.save();

        IPluginContext pluginContext = start(config);

        // open preview
        modelReference.setModel(new JcrNodeModel("/test/content/template"));
        assertEquals(1, getPreviews().size());
        assertEquals(0, getEditors().size());

        // edit type
        IEditorManager editorMgr = context.getService("editor.manager", IEditorManager.class);
        IEditor editor = editorMgr.getEditor(new JcrNodeModel("/test/content/template"));
        editor.setMode(IEditor.Mode.EDIT);
        assertEquals(0, getPreviews().size());
        assertEquals(1, getEditors().size());

        // done editing
        editor.setMode(IEditor.Mode.VIEW);
        assertEquals(1, getPreviews().size());
        assertEquals(0, getEditors().size());
    }

    @Test
    public void testTitlePropagation() throws Exception {
        createDocument("document");
        start(config);

        // open preview
        modelReference.setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(1, getRenderers().size());
        IRenderService renderer = getRenderers().get(0);
        String serviceId = context.getReference(renderer).getServiceId();
        ITitleDecorator decorator = context.getService(serviceId, ITitleDecorator.class);
        assertNotNull(decorator);
        assertEquals("document", decorator.getTitle().getObject());
    }

    @Test
    public void testVersionedDocument() throws Exception {
        createDocument("document");
        session.save();
        start(config);

        Version version = root.getNode("test/content/document").checkin();
        IEditorManager mgr = context.getService("editor.manager", IEditorManager.class);
        try {
            mgr.openEditor(new JcrNodeModel(version));
            fail("Could open editor for Version");
        } catch (ServiceException e) {
            // ignore, this is OK
        }
        IEditor editor = mgr.openPreview(new JcrNodeModel(version));
    }

    @Test
    public void deleteDocumentClosesEditor() throws Exception {
        createDocument("document");
        start(config);

        // open preview
        JcrNodeModel model = new JcrNodeModel("/test/content/document");
        IEditorManager editorMgr = context.getService("editor.manager", IEditorManager.class);
        IEditor editor = editorMgr.openEditor(model);
        assertEquals(model, modelReference.getModel());
        home.detach();
        model.detach();

        session.getRootNode().getNode("test/content").remove();
        session.save();
        home.processEvents();

        assertEquals(0, getPreviews().size());
    }

}
