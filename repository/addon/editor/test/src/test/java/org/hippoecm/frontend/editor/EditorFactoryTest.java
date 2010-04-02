/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.reviewedactions.PublishableDocument;
import org.hippoecm.repository.util.Utilities;
import org.junit.Test;

public class EditorFactoryTest extends PluginTest {

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
                "/test/facetsearch", "hippo:facetsearch",
                    "hippo:docbase", "/test/content",
                    "hippo:queryname", "state",
                    "hippo:facets", HippoStdNodeType.HIPPOSTD_STATE,
                "/test/mirror", "hippo:mirror",
                    "hippo:docbase", "/test/content",
                "/test/plugin", "frontend:pluginconfig",
                    "cluster.edit.name", "editor",
                    "cluster.preview.name", "preview",
                    "cluster.compare.name", "compare",
                    "/test/plugin/cluster.edit.options", "frontend:pluginconfig",
                        "wicket.id", RENDERERS,
                    "/test/plugin/cluster.preview.options", "frontend:pluginconfig",
                        "wicket.id", RENDERERS,
                    "/test/plugin/cluster.compare.options", "frontend:pluginconfig",
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
                    "editor.id", "${editor.id}",
            "/config/test-app/compare", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:references", "model.compareTo",
                "frontend:services", "wicket.id",
                "/config/test-app/compare/plugin", "frontend:plugin",
                    "plugin.class", Comparer.class.getName(),
                    "wicket.id", "${wicket.id}",
                    "wicket.model", "${wicket.model}",
                    "model.compareTo", "${model.compareTo}",
    };

    final static String[] cmstestdocument = new String[] {
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

    final static String[] plaintestdocument = new String[] {
            "/${name}", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
                "/${name}/${name}", "hippo:document",
                    "jcr:mixinTypes", "hippo:harddocument",
            
    };

    IPluginConfig config;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();

        build(session, content1);
        session.save();
        build(session, content2);
        session.save();

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    protected void createDocument(String name) throws RepositoryException {
        Map<String, String> pars = new MiniMap(1);
        pars.put("name", name);
        build(session, mount("/test/content", instantiate(cmstestdocument, pars)));
    }

    @Test
    public void testReviewedActionsEditing() throws Exception {
        createDocument("document");

        EditorFactory factory = new EditorFactory(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW);

        // simulate workflow step "obtainEditableInstance"
        Node unpublished = session.getRootNode().getNode("test/content/document/document");
        Node draft = ((HippoSession) session).copy(unpublished, unpublished.getPath());
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "draft");
        draft.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, CREDENTIALS.getString("username"));
        session.save();

        if (editor instanceof IRefreshable) {
            ((IRefreshable) editor).refresh();
        }
        assertEquals(Mode.EDIT, editor.getMode());
    }

    @Test
    public void testEditPublished() throws Exception {
        createDocument("document");

        Node unpublished = session.getRootNode().getNode("test/content/document/document");
        unpublished.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        session.save();

        EditorFactory factory = new EditorFactory(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW);

        // simulate workflow step "obtainEditableInstance"
        Node draft = ((HippoSession) session).copy(unpublished, unpublished.getPath());
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "draft");
        draft.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, CREDENTIALS.getString("username"));
        session.save();
        ((IRefreshable) editor).refresh();
        assertEquals(Mode.EDIT, editor.getMode());

        // simulate "commitEditableInstance"
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "unpublished");
        session.save();
        ((IRefreshable) editor).refresh();
        assertEquals(Mode.COMPARE, editor.getMode());
    }

    @Test
    public void testSetMode() throws Exception {

        String[] workflowConfig = {
            "/publishable", "hipposys:workflow",
                "hipposys:nodetype", "hippostd:publishable",
                "hipposys:classname", org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflowImpl.class.getName(),
                "hipposys:display", "publishable workflow",
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

        EditorFactory factory = new EditorFactory(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW);

        editor.setMode(IEditor.Mode.EDIT);
        assertEquals(0, getPreviews().size());
        assertEquals(1, getEditors().size());
    }

    @Test
    public void closeFilterIsInvoked() throws Exception {
        createDocument("document");

        EditorFactory factory = new EditorFactory(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW);

        // open editor
        assertEquals(1, getCloseFilters().size());
        List<CloseFilter> filters = getCloseFilters();

        // close editor
        editor.close();
        assertTrue(filters.get(0).closed);
    }


    @Test
    public void testTemplateType() throws Exception {
        Node test = root.getNode("test/content").addNode("template", HippoNodeType.NT_TEMPLATETYPE);
        test.addNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE, HippoNodeType.NT_HANDLE);
        session.save();

        // open preview
        EditorFactory factory = new EditorFactory(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/template"),
                Mode.VIEW);

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
        start(config);

        Version version = root.getNode("test/content/document").checkin();

        EditorFactory factory = new EditorFactory(context, config);
        try {
            factory.newEditor(new TestEditorContext(), new JcrNodeModel(version), Mode.EDIT);
            fail("Could open Version in edit mode");
        } catch (EditorException e) {
            // ignore, this is OK
        }
        try {
            factory.newEditor(new TestEditorContext(), new JcrNodeModel(version), Mode.VIEW);
            fail("Could open Version in view mode");
        } catch (EditorException e) {
            // ignore, this is OK
        }
        factory.newEditor(new TestEditorContext(), new JcrNodeModel(version), Mode.COMPARE);
    }

    @Test
    public void testVersionedDocument() throws Exception {
        Map<String, String> pars = new MiniMap<String, String>(1);
        pars.put("name", "document");
        build(session, mount("/test/content", instantiate(plaintestdocument, pars)));

        session.save();
        start(config);

        Version version = root.getNode("test/content/document").checkin();

        EditorFactory factory = new EditorFactory(context, config);
        IEditor editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel(version), Mode.COMPARE);
        List<IRenderService> comparers = getComparers();
        assertEquals(1, comparers.size());
        Comparer comparer = (Comparer) comparers.get(0);
        JcrNodeModel current = (JcrNodeModel) comparer.getModel();
        JcrNodeModel base = (JcrNodeModel) comparer.getCompareToModel();
        assertEquals(new JcrNodeModel("/test/content/document/document"), current);
        assertEquals(new JcrNodeModel(version.getNode("jcr:frozenNode")), base);
    }

    @Test
    public void testVersionedPublishableDocument() throws Exception {
        createDocument("document");
        session.save();
        start(config);

        Node handle = root.getNode("test/content/document");
        Node document = handle.getNode("document");
        Version docVersion = document.checkin();
        Version handleVersion = handle.checkin();

        handle.checkout();
        Node copy = ((HippoSession) handle.getSession()).copy(handle.getNode("document"), handle.getPath() + "/document");
        copy.setProperty("hippostd:state", "unpublished");
        session.save();

        EditorFactory factory = new EditorFactory(context, config);
        IEditor editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel(handleVersion), Mode.COMPARE);
        List<IRenderService> comparers = getComparers();
        assertEquals(1, comparers.size());
        Comparer comparer = (Comparer) comparers.get(0);
        JcrNodeModel current = (JcrNodeModel) comparer.getModel();
        JcrNodeModel base = (JcrNodeModel) comparer.getCompareToModel();
        assertEquals(new JcrNodeModel(copy), current);
        assertEquals(new JcrNodeModel(docVersion.getNode("jcr:frozenNode")), base);
    }

    @Test
    public void deleteDocumentClosesEditor() throws Exception {
        createDocument("document");
        start(config);

        // open preview
        EditorFactory factory = new EditorFactory(context, config);
        JcrNodeModel model = new JcrNodeModel("/test/content/document");
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), model, Mode.VIEW);

        assertEquals(1, getPreviews().size());

        session.getRootNode().getNode("test/content").remove();

        ((IRefreshable) editor).refresh();

        assertEquals(0, getPreviews().size());
    }

    @Test
    public void testOpenCompareForUnpublished() throws RepositoryException, EditorException {
        build(session, new String[] {
            "/test/document", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
                "/test/document/document", "hippo:document",
                    "jcr:mixinTypes", "hippostd:publishable",
                    "hippostd:state", "unpublished",
                "/test/document/document", "hippo:document",
                    "jcr:mixinTypes", "hippostd:publishable",
                    "hippostd:state", "published",
        });

        EditorFactory factory = new EditorFactory(context, config);
        JcrNodeModel model = new JcrNodeModel("/test/document");
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), model, Mode.VIEW);

        assertEquals(1, getComparers().size());
        assertEquals(IEditor.Mode.COMPARE, editor.getMode());
    }

}
