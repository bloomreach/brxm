/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.standardworkflow;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.easymock.EasyMock;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RenameDocumentDialogTest extends AbstractDocumentDialogTest {

    private void createDialog(final boolean workflowError) throws Exception {
        final WorkflowDescriptorModel workflowDescriptorModel = workflowError ? createErrorWorkflow() : createNormalWorkflow();
        final IWorkflowInvoker invoker = mockWorkflowInvoker();
        final StringCodec stringCodec = new StringCodecFactory.UriEncoding();
        final IModel<StringCodec> stringCodecModel = ReadOnlyModel.of(() -> stringCodec);

        // rename the 'news' folder
        final RenameDocumentArguments renameDocumentArguments = new RenameDocumentArguments("News", "news", HippoStdNodeType.NT_FOLDER);

        final RenameDocumentDialog dialog = new RenameDocumentDialog(
                renameDocumentArguments,
                Model.of("Add document dialog title"),
                invoker,
                stringCodecModel,
                workflowDescriptorModel
        );

        executeDialog(dialog);
    }

    private WorkflowDescriptorModel createNormalWorkflow() throws IOException, RepositoryException {
        final WorkflowDescriptorModel wfdm = EasyMock.createMock(WorkflowDescriptorModel.class);
        final Node contentNode = createContentNode();
        assertNotNull(contentNode);
        EasyMock.expect(wfdm.getNode()).andReturn(contentNode.getNodes().nextNode());
        EasyMock.replay(wfdm);
        return wfdm;
    }

    /**
     * Create a mock folder node containing two folder: News and Common
     */
    private Node createContentNode() throws IOException, RepositoryException {
        final MockNode projectNode = MockNodeUtil.addFolder(root, "myhippoproject");
        MockNodeUtil.addFolder(projectNode, "news", "News");
        MockNodeUtil.addFolder(projectNode, "common", "Common");
        return projectNode;
    }

    @Test
    public void dialogCreatedContainingExpectedInputValues() throws Exception {
        createDialog(false);

        final FormComponent<String> nameComponent = getNameField();
        assertNotNull(nameComponent);
        assertEquals("News", nameComponent.getValue());

        final FormComponent<String> urlField = getUrlField();
        assertNotNull(urlField);
        assertEquals("news", urlField.getValue());

        // url field is editable in renaming dialog
        assertTrue(urlField.isEnabled());

        // the urlAction must be 'Reset'
        final Label urlActionLabel = getUrlActionLabel();
        assertNotNull(urlActionLabel);
        assertEquals("Reset", urlActionLabel.getDefaultModelObject());
    }

    @Test
    public void clickOKWithSameNames() throws Exception {
        createDialog(false);
        clickOkButton();

        tester.assertErrorMessages("No name is changed. Please enter different names.");
    }

    @Test
    public void renameFolderWithSameUriNameExistedLocalizedName() throws Exception {
        createDialog(false);
        setUrl("news");
        setName("Common");
        clickOkButton();

        tester.assertErrorMessages("The name \'Common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithSameUriNameNonExistentLocalizedName() throws Exception {
        createDialog(false);
        setUrl("news");
        setName("Different News");
        clickOkButton();

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithExistedUriNameSameLocalizedName() throws Exception {
        createDialog(false);
        setUrl("common");
        setName("News");
        clickOkButton();

        tester.assertErrorMessages("The URL name \'common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithExistedUriNameNonExistentLocalizedName() throws Exception {
        createDialog(false);
        setUrl("common");
        setName("Different News");
        clickOkButton();

        tester.assertErrorMessages("The URL name \'common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithExistedUriNameAndExistedLocalizedName() throws Exception {
        createDialog(false);
        setUrl("common");
        setName("Common");
        clickOkButton();

        tester.assertErrorMessages("The URL name \'common\' and name \'Common\' are already used in this folder. Please use different names.");
    }

    @Test
    public void renameFolderWithNonExistentUriNameSameLocalizedName() throws Exception {
        createDialog(false);
        setUrl("different-news");
        setName("News");
        clickOkButton();

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithNonExistentUriNameNonExistentLocalizedName() throws Exception {
        createDialog(false);
        setUrl("different-news");
        setName("Different-News");
        clickOkButton();

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithNonExistentUriNameExistedLocalizedName() throws Exception {
        createDialog(false);
        setUrl("different-news");
        setName("Common");
        clickOkButton();

        tester.assertErrorMessages("The name \'Common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithWorkflowException() throws Exception {
        createDialog(true);
        setUrl("another-news");
        setName("Another News");
        clickOkButton();

        tester.assertErrorMessages("Failed to validate input names");
    }
}
