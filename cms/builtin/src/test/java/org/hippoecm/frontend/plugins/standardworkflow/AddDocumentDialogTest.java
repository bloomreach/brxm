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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.repository.api.StringCodec;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AddDocumentDialogTest extends AbstractWicketDialogTest {

    public static final String WICKET_PATH_ENABLE_URIINPUT_LINK = "dialog:content:form:name-url:uriAction";
    public static final String WICKET_PATH_OK_BUTTON = "dialog:content:form:buttons:0:button";
    public static final String URL_INPUT = "name-url:url";
    public static final String NAME_INPUT = "name-url:name";

    private StringCodec stringCodec;

    private FormTester createDialog(boolean workflowError) throws Exception {
        final WorkflowDescriptorModel workflowDescriptorModel = workflowError ? createErrorWorkflow() : createNormalWorkflow();

        final IWorkflowInvoker invoker = mockWorkflowInvoker();

        stringCodec = EasyMock.createMock(StringCodec.class);
        final ILocaleProvider localeProvider = EasyMock.createMock(ILocaleProvider.class);
        EasyMock.expect(localeProvider.getLocales()).andReturn(Collections.emptyList());
        EasyMock.replay(localeProvider);

        IModel<StringCodec> stringCodecModel = new LoadableDetachableModel<StringCodec>(stringCodec) {
            @Override
            protected StringCodec load() {
                return stringCodec;
            }
        };

        final AddDocumentDialog dialog = new AddDocumentDialog(
                new AddDocumentArguments(),
                Model.of("Add document dialog title"),
                "category test",
                new HashSet<>(Arrays.asList(new String[]{"cat1"})),
                false,
                invoker,
                stringCodecModel,
                localeProvider,
                workflowDescriptorModel
        );

        return executeDialog(dialog);
    }

    private WorkflowDescriptorModel createNormalWorkflow() throws IOException, RepositoryException {
        final WorkflowDescriptorModel wfdm = EasyMock.createMock(WorkflowDescriptorModel.class);
        final Node contentNode = createContentNode();
        assertNotNull(contentNode);
        EasyMock.expect(wfdm.getNode()).andReturn(contentNode);
        EasyMock.replay(wfdm);
        return wfdm;
    }

    /**
     * Create a mock folder node containing two folder: News and Common
     */
    public Node createContentNode() throws IOException, RepositoryException {
        final MockNode projectNode = MockNodeUtil.addFolder(root, "myhippoproject");
        MockNodeUtil.addFolder(projectNode, "news", "News");
        MockNodeUtil.addFolder(projectNode, "common", "Common");
        return projectNode;
    }

    @Test
    public void dialogCreatedWithInitialStates() throws Exception {
        final FormTester formTester = createDialog(false);
        FormComponent<String> nameComponent = (FormComponent<String>) formTester.getForm().get(NAME_INPUT);
        assertNotNull(nameComponent);

        FormComponent<String> urlField = (FormComponent<String>) formTester.getForm().get(URL_INPUT);
        assertNotNull(urlField);

        // url field is non-editable
        assertFalse(urlField.isEnabled());

        // the urlAction must be 'Edit'
        Label urlActionLabel = (Label) formTester.getForm().get("name-url:uriAction:uriActionLabel");
        assertNotNull(urlActionLabel);
        assertEquals("Edit", urlActionLabel.getDefaultModelObject());
    }


    @Test
    public void clickOKWithEmptyInputs() throws Exception {
        final FormTester formTester = createDialog(false);

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("'name' is required.");
    }

    @Test
    public void addFolderWithNewNames() throws Exception {
        final FormTester formTester = createDialog(false);

        EasyMock.expect(stringCodec.encode(eq("archives"))).andReturn("archives");
        EasyMock.replay(stringCodec);

        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);
        formTester.setValue(URL_INPUT, "archives");
        formTester.setValue(NAME_INPUT, "Archives");

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertNoErrorMessage();
    }

    @Test
    public void addFolderWithExistedUriName() throws Exception {
        final FormTester formTester = createDialog(false);

        // disable the translation from Name to uriName
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);

        formTester.setValue(URL_INPUT, "news");
        formTester.setValue(NAME_INPUT, "Another News");

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'news\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void addFolderWithExistingLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);

        // disable the translation from Name to uriName
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);

        formTester.setValue(URL_INPUT, "news-123");
        formTester.setValue(NAME_INPUT, "News");

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The name \'News\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void addFolderWithExistedUriNameAndLocalizedName() throws Exception {
        final FormTester formTester = createDialog(false);

        // disable the translation from Name to uriName
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);

        formTester.setValue(URL_INPUT, "news");
        formTester.setValue(NAME_INPUT, "News");

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'news\' and name \'News\' are already used in this folder. Please use different names.");
    }

    @Test
    public void addFolderWithWorkflowException() throws Exception {
        final FormTester formTester = createDialog(true);

        // disable the translation from Name to uriName
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);

        formTester.setValue(URL_INPUT, "another-news");
        formTester.setValue(NAME_INPUT, "Another News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("Failed to validate input names");
    }
}