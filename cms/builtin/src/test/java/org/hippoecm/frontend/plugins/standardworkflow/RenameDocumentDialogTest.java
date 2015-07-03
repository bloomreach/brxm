/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.DummyPlugin;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RenameDocumentDialogTest {
    public static final String WICKET_PATH_OK_BUTTON = "dialog:content:form:buttons:0:button";
    public static final String URL_INPUT = "name-url:url";
    public static final String NAME_INPUT = "name-url:name";

    private MockNode root;
    private HippoTester tester;
    private PluginPage home;
    private PluginContext context;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();

        tester = new HippoTester();
        tester.getSession().setLocale(Locale.ENGLISH);

        home = (PluginPage) tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = home.getPluginManager().start(config);
    }


    private FormTester createDialog(boolean workflowError){
        final WorkflowDescriptorModel workflowDescriptorModel;
        try {
            workflowDescriptorModel = workflowError ? createErrorWorkflow() : createNormalWorkflow();
        } catch (RepositoryException | IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }

        // rename the 'news' folder
        RenameDocumentArguments docModel = new RenameDocumentArguments();
        docModel.setUriName("news");
        docModel.setTargetName("News");
        docModel.setNodeType(HippoStdNodeType.NT_FOLDER);

        final IWorkflowInvoker invoker = Mockito.mock(IWorkflowInvoker.class);

        try {
            Mockito.doNothing().when(invoker).invokeWorkflow();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final StringCodec stringCodec = new StringCodecFactory.UriEncoding();
        IModel<StringCodec> stringCodecModel = new LoadableDetachableModel<StringCodec>(stringCodec) {
            @Override
            protected StringCodec load() {
                return stringCodec;
            }
        };

        RenameDocumentDialog dialog = new RenameDocumentDialog(
                docModel,
                Model.of("Add document dialog title"),
                invoker,
                stringCodecModel,
                workflowDescriptorModel
        );
        tester.runInAjax(home, new Runnable() {
            @Override
            public void run() {
                IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
                dialogService.show(dialog);
            }

        });
        return tester.newFormTester("dialog:content:form");
    }

    private WorkflowDescriptorModel createNormalWorkflow() throws IOException, RepositoryException {
        final WorkflowDescriptorModel wfdm = mock(WorkflowDescriptorModel.class);
        final Node contentNode = createContentNode();
        assertNotNull(contentNode);
        when(wfdm.getNode()).thenReturn(contentNode.getNode(HippoNodeType.HIPPO_TRANSLATION));
        return wfdm;
    }

    private WorkflowDescriptorModel createErrorWorkflow() throws RepositoryException {
        final WorkflowDescriptorModel wfdm = mock(WorkflowDescriptorModel.class);
        when(wfdm.getNode()).thenThrow(new RepositoryException("No node found"));
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
    public void dialogCreatedContainingExpectedInputValues(){
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
    public void clickOKWithSameNames(){
        final FormTester formTester = createDialog(false);
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("No name is changed. Please enter different names.");
    }

    @Test
    public void renameFolderWithSameUriNameExistedLocalizedName(){
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "news");
        formTester.setValue(NAME_INPUT, "Common");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertErrorMessages("The name \'Common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithSameUriNameNonExistentLocalizedName(){
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "news");
        formTester.setValue(NAME_INPUT, "Different News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithExistedUriNameSameLocalizedName(){
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "common");
        formTester.setValue(NAME_INPUT, "News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithExistedUriNameNonExistentLocalizedName(){
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "common");
        formTester.setValue(NAME_INPUT, "Different News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithExistedUriNameAndExistedLocalizedName(){
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "common");
        formTester.setValue(NAME_INPUT, "Common");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'common\' and name \'Common\' are already used in this folder. Please use different names.");
    }

    @Test
    public void renameFolderWithNonExistentUriNameSameLocalizedName(){
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "different-news");
        formTester.setValue(NAME_INPUT, "News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithNonExistentUriNameNonExistentLocalizedName(){
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "different-news");
        formTester.setValue(NAME_INPUT, "Different-News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertNoErrorMessage();
    }

    @Test
    public void renameFolderWithNonExistentUriNameExistedLocalizedName(){
        final FormTester formTester = createDialog(false);
        formTester.setValue(URL_INPUT, "different-news");
        formTester.setValue(NAME_INPUT, "Common");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");

        tester.assertErrorMessages("The name \'Common\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void renameFolderWithWorkflowException(){
        final FormTester formTester = createDialog(true);

        formTester.setValue(URL_INPUT, "another-news");
        formTester.setValue(NAME_INPUT, "Another News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("Error to validate input names");
    }
}