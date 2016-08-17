/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.easymock.EasyMock;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RenameDocumentDialogTest extends AbstractWicketDialogTest {

    public static final String WICKET_PATH_OK_BUTTON = "dialog:content:form:buttons:0:button";
    public static final String URL_INPUT = "name-url:url";
    public static final String NAME_INPUT = "name-url:name";

    private FormTester createDialog(boolean workflowError) throws Exception {
        final WorkflowDescriptorModel workflowDescriptorModel = workflowError ? createErrorWorkflow() : createNormalWorkflow();

        final IWorkflowInvoker invoker = mockWorkflowInvoker();

        final StringCodec stringCodec = new StringCodecFactory.UriEncoding();
        IModel<StringCodec> stringCodecModel = new LoadableDetachableModel<StringCodec>(stringCodec) {
            @Override
            protected StringCodec load() {
                return stringCodec;
            }
        };

        // rename the 'news' folder
        final RenameDocumentArguments renameDocumentArguments = new RenameDocumentArguments("News", "news", HippoStdNodeType.NT_FOLDER);

        final RenameDocumentDialog dialog = new RenameDocumentDialog(
                renameDocumentArguments,
                Model.of("Add document dialog title"),
                invoker,
                stringCodecModel,
                workflowDescriptorModel
        );

        return executeDialog(dialog);
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
        final FormTester formTester = createDialog(false);
        FormComponent<String> nameComponent = (FormComponent<String>) formTester.getForm().get(NAME_INPUT);
        assertNotNull(nameComponent);
        assertEquals("News", nameComponent.getValue());

        FormComponent<String> urlField = (FormComponent<String>) formTester.getForm().get(URL_INPUT);
        assertNotNull(urlField);
        assertEquals("news", urlField.getValue());

        // url field is editable in renaming dialog
        assertTrue(urlField.isEnabled());

        // the urlAction must be 'Reset'
        Label urlActionLabel = (Label) formTester.getForm().get("name-url:uriAction:uriActionLabel");
        assertNotNull(urlActionLabel);
        assertEquals("Reset", urlActionLabel.getDefaultModelObject());
    }

    @Test
    public void clickOKWithSameNames() throws Exception {
        final FormTester formTester = createDialog(false);
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("No name is changed. Please enter different names.");
    }

    @Test
    public void renameFolderWithSameUriNameExistedLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "news");
        formTester.setValue(NAME_INPUT, "Common");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertErrorMessages("The name \'Common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithSameUriNameNonExistentLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "news");
        formTester.setValue(NAME_INPUT, "Different News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithExistedUriNameSameLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "common");
        formTester.setValue(NAME_INPUT, "News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithExistedUriNameNonExistentLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "common");
        formTester.setValue(NAME_INPUT, "Different News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithExistedUriNameAndExistedLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "common");
        formTester.setValue(NAME_INPUT, "Common");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'common\' and name \'Common\' are already used in this folder. Please use different names.");
    }

    @Test
    public void renameFolderWithNonExistentUriNameSameLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "different-news");
        formTester.setValue(NAME_INPUT, "News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithNonExistentUriNameNonExistentLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "different-news");
        formTester.setValue(NAME_INPUT, "Different-News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithNonExistentUriNameExistedLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "different-news");
        formTester.setValue(NAME_INPUT, "Common");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertErrorMessages("The name \'Common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithWorkflowException() throws Exception {
        final FormTester formTester = createDialog(true);

        formTester.setValue(URL_INPUT, "another-news");
        formTester.setValue(NAME_INPUT, "Another News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("Failed to validate input names");
    }
}