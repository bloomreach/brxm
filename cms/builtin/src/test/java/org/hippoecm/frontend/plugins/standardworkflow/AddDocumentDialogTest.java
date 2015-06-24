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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.repository.api.StringCodec;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddDocumentDialogTest {

    public static final String WICKET_PATH_ENABLE_URIINPUT_LINK = "dialog:content:form:name-url:uriAction";
    public static final String WICKET_PATH_OK_BUTTON = "dialog:content:form:buttons:0:button";
    public static final String URL_INPUT = "name-url:url";
    public static final String NAME_INPUT = "name-url:name";

    HippoTester tester;
    private PluginPage home;
    private PluginContext context;
    private MockNode root;

    @Before
    public void setUp() throws RepositoryException, IOException {
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

        AddDocumentArguments addDocumentModel = new AddDocumentArguments();
        final IWorkflowInvoker invoker = mock(IWorkflowInvoker.class);

        try {
            Mockito.doNothing().when(invoker).invokeWorkflow();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final StringCodec stringCodec = mock(StringCodec.class);
        final ILocaleProvider localProvider = mock(ILocaleProvider.class);

        IModel<StringCodec> stringCodecModel = new LoadableDetachableModel<StringCodec>(stringCodec) {
            @Override
            protected StringCodec load() {
                return stringCodec;
            }
        };

        boolean translated = false;
        final AddDocumentDialog dialog = new AddDocumentDialog(
                addDocumentModel,
                Model.of("Add document dialog title"),
                "category test",
                new HashSet<>(Arrays.asList(new String[]{"cat1"})),
                translated,
                invoker,
                stringCodecModel,
                localProvider,
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

    private WorkflowDescriptorModel createErrorWorkflow() throws RepositoryException {
        final WorkflowDescriptorModel wfdm = mock(WorkflowDescriptorModel.class);
        when(wfdm.getNode()).thenThrow(new RepositoryException("No node found"));
        return wfdm;
    }

    private WorkflowDescriptorModel createNormalWorkflow() throws IOException, RepositoryException {
        final WorkflowDescriptorModel wfdm = mock(WorkflowDescriptorModel.class);
        final Node contentNode = createContentNode();
        assertNotNull(contentNode);
        when(wfdm.getNode()).thenReturn(contentNode);
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
    public void clickOKWithEmptyInputs(){
        final FormTester formTester = createDialog(false);

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("'name' is required.");
    }

    @Test
    public void addFolderWithNewNames(){
        final FormTester formTester = createDialog(false);
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);
        formTester.setValue(URL_INPUT, "archives");
        formTester.setValue(NAME_INPUT, "Archives");

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertNoErrorMessage();
    }

    @Test
    public void addFolderWithExistedUriName(){
        final FormTester formTester = createDialog(false);

        // disable the translation from Name to uriName
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);

        formTester.setValue(URL_INPUT, "news");
        formTester.setValue(NAME_INPUT, "Another News");

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'news\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void addFolderWithExistedLocalizedName(){
        final FormTester formTester = createDialog(false);

        // disable the translation from Name to uriName
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);

        formTester.setValue(URL_INPUT, "news-123");
        formTester.setValue(NAME_INPUT, "News");

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The name \'News\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void addFolderWithExistedUriNameAndLocalizedName(){
        final FormTester formTester = createDialog(false);

        // disable the translation from Name to uriName
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);

        formTester.setValue(URL_INPUT, "news");
        formTester.setValue(NAME_INPUT, "News");

        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("The URL name \'news\' and name \'News\' are already used in this folder. Please use different names.");
    }

    @Test
    public void addFolderWithWorkflowException(){
        final FormTester formTester = createDialog(true);

        // disable the translation from Name to uriName
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);

        formTester.setValue(URL_INPUT, "another-news");
        formTester.setValue(NAME_INPUT, "Another News");
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "onclick");
        tester.assertErrorMessages("Error to validate input names");
    }
}