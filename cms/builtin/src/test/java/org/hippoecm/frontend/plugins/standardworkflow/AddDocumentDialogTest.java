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
import java.util.Locale;

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
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AddDocumentDialogTest {

    public static final String WICKET_PATH_ENABLE_URIINPUT_LINK = "dialog:content:form:name-url:uriAction";
    public static final String WICKET_PATH_OK_BUTTON = "dialog:content:form:buttons:0:button";
    public static final String URL_INPUT = "name-url:url";
    public static final String NAME_INPUT = "name-url:name";

    private HippoTester tester;
    private PluginPage home;
    private PluginContext context;
    private MockNode root;
    private StringCodec stringCodec;

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

    private FormTester createDialog(boolean workflowError) throws Exception {
        final WorkflowDescriptorModel workflowDescriptorModel;
        workflowDescriptorModel = workflowError ? createErrorWorkflow() : createNormalWorkflow();

        final AddDocumentArguments addDocumentArguments = new AddDocumentArguments();
        final IWorkflowInvoker invoker = EasyMock.createMock(IWorkflowInvoker.class);

        invoker.invokeWorkflow();
        EasyMock.expectLastCall();

        stringCodec = EasyMock.createMock(StringCodec.class);
        final ILocaleProvider localeProvider = EasyMock.createMock(ILocaleProvider.class);
        EasyMock.expect(localeProvider.getLocales()).andReturn(Collections.emptyList());

        EasyMock.replay(invoker, localeProvider);

        IModel<StringCodec> stringCodecModel = new LoadableDetachableModel<StringCodec>(stringCodec) {
            @Override
            protected StringCodec load() {
                return stringCodec;
            }
        };

        final AddDocumentDialog dialog = new AddDocumentDialog(
                addDocumentArguments,
                Model.of("Add document dialog title"),
                "category test",
                new HashSet<>(Arrays.asList(new String[]{"cat1"})),
                false,
                invoker,
                stringCodecModel,
                localeProvider,
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
        final WorkflowDescriptorModel wfdm = EasyMock.createMock(WorkflowDescriptorModel.class);
        EasyMock.expect(wfdm.getNode()).andThrow(new RepositoryException("No node found"));
        EasyMock.replay(wfdm);
        return wfdm;
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