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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.wicket.model.IModel;
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
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EditorFactoryTest extends PluginTest {

    static final String EDITORS = "editors";
    static final String PREVIEWS = "previews";
    static final String FILTERS = "filters";
    static final String RENDERERS = "service.renderer";
    
    public static class Editor extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public Editor(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, EDITORS);
        }
    }

    public static class Preview extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public Preview(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, PREVIEWS);
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
            "/config/test-app/cms-editor", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:services", "wicket.id",
                "/config/test-app/cms-editor/plugin", "frontend:plugin",
                    "plugin.class", Editor.class.getName(),
            "/config/test-app/cms-preview", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:references", "model.compareTo",
                "frontend:references", "editor.id",
                "frontend:services", "wicket.id",
                "/config/test-app/cms-preview/plugin", "frontend:plugin",
                    "plugin.class", Preview.class.getName(),
                "/config/test-app/cms-preview/filter", "frontend:plugin",
                    "plugin.class", CloseFilter.class.getName(),
    };

    final static String[] cmstestdocument = new String[] {
            "/${name}", "hippo:handle",
                "jcr:mixinTypes", "mix:referenceable",
                "/${name}/${name}", "cmstest:document",
                    "jcr:mixinTypes", "mix:versionable",
                    "jcr:mixinTypes", "hippostdpubwf:document",
                    "jcr:mixinTypes", "hippostd:publishableSummary",
                    HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.UNPUBLISHED,
                    HippoStdNodeType.HIPPOSTD_STATESUMMARY, HippoStdNodeType.NEW,
                    "hippostdpubwf:createdBy", "admin",
                    "hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00",
                    "hippostdpubwf:lastModifiedBy", "admin",
                    "hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00"
            
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
    }

    @Override
    public void tearDown() throws Exception {
        String path = "/hippo:configuration/hippo:workflows/default/publishable";
        if (session.nodeExists(path)) {
            session.getNode(path).remove();
            session.save();
        }
        super.tearDown();
    }

    protected void createDocument(String name) throws RepositoryException {
        Map<String, String> pars = new MiniMap(1);
        pars.put("name", name);
        build(session, mount("/test/content", instantiate(cmstestdocument, pars)));
    }

    @Test
    public void testReviewedActionsEditing() throws Exception {
        createDocument("document");

        HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW, parameters);

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

        HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW, parameters);

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
        };
        build(session, mount("/hippo:configuration/hippo:workflows/default", workflowConfig));
        Node category = session.getRootNode().getNode("hippo:configuration/hippo:workflows/default");
        category.orderBefore("publishable", category.getNodes().nextNode().getName());

        createDocument("document");

        session.save();

        HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                 Mode.VIEW, parameters);

        editor.setMode(IEditor.Mode.EDIT);
        assertEquals(0, getPreviews().size());
        assertEquals(1, getEditors().size());
    }

    @Test
    public void testVersionedPublishableDocument() throws Exception {
        createDocument("document");
        session.save();

        Node handle = root.getNode("test/content/document");
        Node document = handle.getNode("document");
        Version docVersion = document.checkin();

        Node copy = ((HippoSession) handle.getSession()).copy(handle.getNode("document"), handle.getPath() + "/document");
        copy.setProperty("hippostd:state", "unpublished");
        session.save();

        HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        IEditor editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel(docVersion), Mode.COMPARE, parameters);
        List<IRenderService> comparers = getPreviews();
        assertEquals(1, comparers.size());
        Preview preview = (Preview) comparers.get(0);
        JcrNodeModel current = (JcrNodeModel) preview.getModel();
        JcrNodeModel base = (JcrNodeModel) preview.getCompareToModel();
        assertEquals(new JcrNodeModel(copy), current);
        assertEquals(new JcrNodeModel(docVersion.getNode("jcr:frozenNode")), base);
    }

    @Test
    public void testOpenCompareForUnpublished() throws RepositoryException, EditorException {
        build(session, new String[] {
            "/test/document", "hippo:handle",
                "jcr:mixinTypes", "mix:referenceable",
                "/test/document/document", "hippo:document",
                    "jcr:mixinTypes", "hippostd:publishable",
                    "hippostd:state", "unpublished",
                "/test/document/document", "hippo:document",
                    "jcr:mixinTypes", "hippostd:publishable",
                    "hippostd:state", "published",
        });

        HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        JcrNodeModel model = new JcrNodeModel("/test/document");
        IEditor<Node> editor = factory.newEditor(new TestEditorContext(), model, Mode.VIEW, parameters);

        assertEquals(1, getPreviews().size());
        assertEquals(IEditor.Mode.COMPARE, editor.getMode());
    }

}
