/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.TestEditorContext;
import org.hippoecm.frontend.TestEditorContext.Preview;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflowImpl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hippoecm.frontend.TestEditorContext.CMS_TEST_DOCUMENT;
import static org.hippoecm.frontend.TestEditorContext.CONTENT_1;
import static org.hippoecm.frontend.TestEditorContext.CONTENT_2;
import static org.hippoecm.frontend.TestEditorContext.EDITORS;
import static org.hippoecm.frontend.TestEditorContext.PREVIEWS;
import static org.junit.Assert.assertThat;

public class EditorFactoryTest extends PluginTest {

    private static final String RENDERERS = "service.renderer";

    private List<IRenderService> getPreviews() {
        return context.getServices(PREVIEWS, IRenderService.class);
    }

    private List<IRenderService> getEditors() {
        return context.getServices(EDITORS, IRenderService.class);
    }

    private static final IPluginConfig PARAMETERS;

    static {
        PARAMETERS = new JavaPluginConfig();
        PARAMETERS.put("wicket.id", RENDERERS);
    }

    private IPluginConfig config;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        build(session, CONTENT_1);
        session.save();
        build(session, CONTENT_2);
        session.save();

        config = new JavaPluginConfig("plugin");
    }

    @Override
    public void tearDown() throws Exception {
        final String path = "/hippo:configuration/hippo:workflows/default/publishable";
        if (session.nodeExists(path)) {
            session.getNode(path).remove();
            session.save();
        }
        super.tearDown();
    }

    private void createDocument(final String name) throws RepositoryException {
        final Map<String, String> pars = new MiniMap(1);
        pars.put("name", name);
        build(session, mount("/test/content", instantiate(CMS_TEST_DOCUMENT, pars)));
    }

    @Test
    public void testReviewedActionsEditing() throws Exception {
        createDocument("document");

        final HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        final IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW, PARAMETERS);

        // simulate workflow step "obtainEditableInstance"
        final Node unpublished = session.getRootNode().getNode("test/content/document/document");
        final Node draft = ((HippoSession) session).copy(unpublished, unpublished.getPath());
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "draft");
        draft.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, USER_CREDENTIALS.getUsername());
        session.save();

        if (editor != null) {
            ((IRefreshable) editor).refresh();
        }
        assertThat(editor.getMode(), is(equalTo(Mode.EDIT)));
    }

    @Test
    public void testEditPublished() throws Exception {
        createDocument("document");

        final Node unpublished = session.getRootNode().getNode("test/content/document/document");
        unpublished.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        session.save();

        final HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        final IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW, PARAMETERS);

        // simulate workflow step "obtainEditableInstance"
        final Node draft = ((HippoSession) session).copy(unpublished, unpublished.getPath());
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "draft");
        draft.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, USER_CREDENTIALS.getUsername());
        session.save();
        ((IRefreshable) editor).refresh();
        assertThat(editor.getMode(), is(equalTo(Mode.EDIT)));

        // simulate "commitEditableInstance"
        draft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "unpublished");
        session.save();
        ((IRefreshable) editor).refresh();
        assertThat(editor.getMode(), is(equalTo(Mode.COMPARE)));
    }

    @Test
    public void testSetMode() throws Exception {
        final String[] workflowConfig = {
                "/publishable", "hipposys:workflow",
                "hipposys:nodetype", "hippo:handle",
                "hipposys:subtype", "hippostd:publishable",
                "hipposys:classname", DocumentWorkflowImpl.class.getName(),
                "hipposys:display", "publishable workflow",
        };
        build(session, mount("/hippo:configuration/hippo:workflows/default", workflowConfig));
        final Node category = session.getRootNode().getNode("hippo:configuration/hippo:workflows/default");
        category.orderBefore("publishable", category.getNodes().nextNode().getName());

        createDocument("document");

        session.save();

        final HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        final IEditor<Node> editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel("/test/content/document"),
                Mode.VIEW, PARAMETERS);

        editor.setMode(Mode.EDIT);
        assertThat(getPreviews().size(), is(equalTo(0)));
        assertThat(getEditors().size(), is(equalTo(1)));
    }

    @Test
    public void testVersionedPublishableDocument() throws Exception {
        createDocument("document");
        session.save();

        final Node handle = root.getNode("test/content/document");
        final Node document = handle.getNode("document");
        final Version docVersion = document.checkin();

        final Node copy = ((HippoSession) handle.getSession()).copy(handle.getNode("document"), handle.getPath() + "/document");
        copy.setProperty("hippostd:state", "unpublished");
        session.save();

        final HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        final IEditor editor = factory.newEditor(new TestEditorContext(), new JcrNodeModel(docVersion), Mode.COMPARE, PARAMETERS);
        final List<IRenderService> comparers = getPreviews();
        assertThat(comparers.size(), is(equalTo(1)));
        final Preview preview = (Preview) comparers.get(0);
        final JcrNodeModel current = (JcrNodeModel) preview.getModel();
        final JcrNodeModel base = (JcrNodeModel) preview.getCompareToModel();
        assertThat(current, is(equalTo(new JcrNodeModel(copy))));
        assertThat(base, is(equalTo(new JcrNodeModel(docVersion.getNode("jcr:frozenNode")))));
    }

    @Test
    public void testOpenCompareForUnpublished() throws RepositoryException, EditorException {
        build(session, new String[]{
                "/test/document", "hippo:handle",
                "jcr:mixinTypes", "mix:referenceable",
                "/test/document/document", "hippo:document",
                "jcr:mixinTypes", "hippostd:publishable",
                "hippostd:state", "unpublished",
                "/test/document/document", "hippo:document",
                "jcr:mixinTypes", "hippostd:publishable",
                "hippostd:state", "published",
        });

        final HippostdEditorFactoryPlugin factory = new HippostdEditorFactoryPlugin(context, config);
        final JcrNodeModel model = new JcrNodeModel("/test/document");
        final IEditor<Node> editor = factory.newEditor(new TestEditorContext(), model, Mode.VIEW, PARAMETERS);

        assertThat(getPreviews().size(), is(equalTo(1)));
        assertThat(editor.getMode(), is(equalTo(Mode.COMPARE)));
    }

}
