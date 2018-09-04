/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.TestEditorContext;
import org.hippoecm.frontend.editor.HippostdPublishableEditor.WorkflowState;
import org.hippoecm.frontend.model.BranchIdModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hippoecm.frontend.TestEditorContext.CMS_TEST_DOCUMENT;
import static org.hippoecm.frontend.TestEditorContext.CONTENT_1;
import static org.hippoecm.frontend.TestEditorContext.CONTENT_2;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verify;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({HippostdPublishableEditor.class, AbstractCmsEditor.class})
public class HippostdPublishableEditorTest extends PluginTest {


    private IPluginConfig config;
    private JcrNodeModel model;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        setupSession();
        setupMocks();
    }

    private void initializeBranchIdModel(String uuid) {
        final BranchIdModel branchIdModel = new BranchIdModel(context, uuid);
        branchIdModel.setInitialBranchInfo("master","core");

    }

    private void setupMocks() throws RepositoryException {
        PowerMock.mockStatic(AbstractCmsEditor.class);
        PowerMock.mockStatic(HippostdPublishableEditor.class);

        final String identifier = createDocument("document");
        initializeBranchIdModel(identifier);

        model = new JcrNodeModel("/test/content/document");
        expect(AbstractCmsEditor.getMyName(anyObject())).andReturn("bah");
    }

    private void setupSession() throws RepositoryException {
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

    private String createDocument(final String name) throws RepositoryException {
        final Map<String, String> pars = new MiniMap<>(1);
        pars.put("name", name);
        build(session, mount("/test/content", instantiate(CMS_TEST_DOCUMENT, pars)));
        final Node node = session.getNode("/test/content/" + name);
        return node.getIdentifier();
    }

    @Test(expected = EditorException.class)
    public void testEditorModelNT_VersionEditError() throws RepositoryException, EditorException {
        expect(HippostdPublishableEditor.getMode(anyObject())).andReturn(Mode.EDIT);
        expect(HippostdPublishableEditor.getWorkflowState(anyObject(),anyObject())).andReturn(new WorkflowState());
        replayAll();

        final HippostdPublishableEditor editor = new HippostdPublishableEditor(new TestEditorContext(), context, config, model);

        editor.getEditorModel();
    }

    @Test
    public void testEditorModelNtVersionEditValid() throws RepositoryException, EditorException {
        expect(HippostdPublishableEditor.getMode(anyObject())).andReturn(Mode.EDIT);
        final WorkflowState state = new WorkflowState();
        final IModel<Node> draft = new JcrNodeModel("/test/content/document/draft");
        state.setDraft(draft);
        state.setHolder(true);
        expect(HippostdPublishableEditor.getWorkflowState(anyObject(),anyObject())).andReturn(state);
        replayAll();

        final HippostdPublishableEditor editor = new HippostdPublishableEditor(new TestEditorContext(), context, config, model);

        final IModel<Node> editorModel = editor.getEditorModel();
        verify(HippostdPublishableEditor.class);
        assertThat(editorModel, is(equalTo(draft)));
    }

    @Test
    public void testEditorModelViewUnpublished() throws RepositoryException, EditorException {
        expect(HippostdPublishableEditor.getMode(anyObject())).andReturn(Mode.VIEW);
        final WorkflowState state = new WorkflowState();
        final IModel<Node> unpublished = new JcrNodeModel("/test/content/document/unpublished");
        state.setUnpublished(unpublished);
        expect(HippostdPublishableEditor.getWorkflowState(anyObject(),anyObject())).andReturn(state);
        replayAll();

        final HippostdPublishableEditor editor = new HippostdPublishableEditor(new TestEditorContext(), context, config, model);

        final IModel<Node> editorModel = editor.getEditorModel();
        verify(HippostdPublishableEditor.class);
        assertThat(editorModel, is(equalTo(unpublished)));
    }

    @Test
    public void testEditorModelViewPublished() throws RepositoryException, EditorException {
        expect(HippostdPublishableEditor.getMode(anyObject())).andReturn(Mode.VIEW);
        final WorkflowState state = new WorkflowState();
        final IModel<Node> published = new JcrNodeModel("/test/content/document/published");
        state.setUnpublished(null);
        state.setPublished(published);
        expect(HippostdPublishableEditor.getWorkflowState(anyObject(),anyObject())).andReturn(state);
        replayAll();

        final HippostdPublishableEditor editor = new HippostdPublishableEditor(new TestEditorContext(), context, config, model);

        final IModel<Node> editorModel = editor.getEditorModel();
        verify(HippostdPublishableEditor.class);
        assertThat(editorModel, is(equalTo(published)));
    }

    @Test
    public void testEditorModelViewDraft() throws RepositoryException, EditorException {
        expect(HippostdPublishableEditor.getMode(anyObject())).andReturn(Mode.VIEW);
        final WorkflowState state = new WorkflowState();
        final IModel<Node> draft = new JcrNodeModel("/test/content/document/draft");
        state.setUnpublished(null);
        state.setPublished(null);
        state.setDraft(draft);
        expect(HippostdPublishableEditor.getWorkflowState(anyObject(),anyObject())).andReturn(state);
        replayAll();

        final HippostdPublishableEditor editor = new HippostdPublishableEditor(new TestEditorContext(), context, config, model);

        final IModel<Node> editorModel = editor.getEditorModel();
        verify(HippostdPublishableEditor.class);
        assertThat(editorModel, is(equalTo(draft)));
    }

    @Test(expected = EditorException.class)
    public void testEditorModelCompareNotNtVersion() throws RepositoryException, EditorException {
        expect(HippostdPublishableEditor.getMode(anyObject())).andReturn(Mode.COMPARE);
        expect(HippostdPublishableEditor.getWorkflowState(anyObject(),anyObject())).andReturn(new WorkflowState());
        replayAll();

        final HippostdPublishableEditor editor = new HippostdPublishableEditor(new TestEditorContext(), context, config, model);
        editor.getEditorModel();
    }

    @Test
    public void testEditorModelNtVersionPublished() throws RepositoryException, EditorException {
        expect(HippostdPublishableEditor.getMode(anyObject())).andReturn(Mode.COMPARE);
        final WorkflowState state = new WorkflowState();

        final IModel<Node> draft = new JcrNodeModel("/test/content/document/draft");
        state.setDraft(draft);

        final IModel<Node> unpublished = new JcrNodeModel("/test/content/document/unpublished");
        state.setUnpublished(unpublished);

        final IModel<Node> published = new JcrNodeModel("/test/content/document/published");
        state.setPublished(published);

        expect(HippostdPublishableEditor.getWorkflowState(anyObject(),anyObject())).andReturn(state);
        replayAll();

        final HippostdPublishableEditor editor = new HippostdPublishableEditor(new TestEditorContext(), context, config, model);

        final IModel<Node> editorModel = editor.getEditorModel();
        verify(HippostdPublishableEditor.class);
        assertThat(editorModel, is(equalTo(unpublished)));
    }

}
