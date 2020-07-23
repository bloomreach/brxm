/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.easymock.EasyMock;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.junit.Test;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AddDocumentDialogTest extends AbstractDocumentDialogTest {

    private StringCodec stringCodec;
    private static List<XPageLayout> xPageLayoutList =   Stream.of(
            new XPageLayout("layout1", "Layout 1",new JcrTemplateNode()),
            new XPageLayout("layout2", "Layout 2", new JcrTemplateNode()),
            new XPageLayout("layout3", "Layout 3", new JcrTemplateNode()))
            .collect(Collectors.toList());

    private void createDialog(final boolean workflowError, List<XPageLayout> xPageLayoutList) throws Exception {
        final WorkflowDescriptorModel workflowDescriptorModel = workflowError ? createErrorWorkflow() : createNormalWorkflow();

        final IWorkflowInvoker invoker = mockWorkflowInvoker();

        stringCodec = EasyMock.createMock(StringCodec.class);
        final ILocaleProvider localeProvider = EasyMock.createMock(ILocaleProvider.class);
        EasyMock.expect(localeProvider.getLocales()).andReturn(Collections.emptyList());
        EasyMock.replay(localeProvider);

        final IModel<StringCodec> stringCodecModel = ReadOnlyModel.of(() -> stringCodec);
        final AddDocumentDialog dialog = new AddDocumentDialog(
                new AddDocumentArguments(),
                Model.of("Add document dialog title"),
                "category test",
                new HashSet<>(Collections.singletonList("cat1")),
                new ListModel(xPageLayoutList),
                false,
                invoker,
                stringCodecModel,
                localeProvider,
                workflowDescriptorModel
        );

        executeDialog(dialog);
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
    private Node createContentNode() throws IOException, RepositoryException {
        final MockNode projectNode = MockNodeUtil.addFolder(root, "myhippoproject");
        MockNodeUtil.addFolder(projectNode, "news", "News");
        MockNodeUtil.addFolder(projectNode, "common", "Common");
        return projectNode;
    }

    @Test
    public void dialogCreatedWithInitialStates() throws Exception {
        createDialog(false, xPageLayoutList);

        final FormComponent<String> nameComponent = getNameField();
        assertNotNull(nameComponent);

        final FormComponent<String> urlField = getUrlField();
        assertNotNull(urlField);

        // url field is non-editable
        assertFalse(urlField.isEnabled());

        // the urlAction must be 'Edit'
        final Label urlActionLabel = getUrlActionLabel();
        assertNotNull(urlActionLabel);
        assertEquals("Edit", urlActionLabel.getDefaultModelObject());
    }

    @Test
    public void clickOKWithEmptyInputs() throws Exception {
        createDialog(false, xPageLayoutList);
        clickOkButton();

        tester.assertErrorMessages("'name' is required.", "'???x-page-layout???' is required.");
    }

    @Test
    public void clickOKWithEmptyInputs_NoXPage() throws Exception {
        createDialog(false, Collections.emptyList());
        clickOkButton();

        tester.assertErrorMessages("'name' is required.");
    }

    @Test
    public void addFolderWithNewNames() throws Exception {
        createDialog(false, xPageLayoutList);

        EasyMock.expect(stringCodec.encode(eq("archives"))).andReturn("archives");
        EasyMock.replay(stringCodec);

        clickUrlActionLink();
        setUrl("archives");

        setName("Archives");
        setXPageLayout("layout1");
        clickOkButton();

        tester.assertNoErrorMessage();
    }

    @Test
    public void addFolderWithExistedUriName() throws Exception {
        createDialog(false, xPageLayoutList);

        // disable the translation from Name to uriName
        clickUrlActionLink();
        setUrl("news");
        setName("Another News");
        setXPageLayout("layout1");
        clickOkButton();

        tester.assertErrorMessages("The URL name \'news\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void addFolderWithExistingLocalizedName() throws Exception {
        createDialog(false, xPageLayoutList);

        // disable the translation from Name to uriName
        clickUrlActionLink();
        setUrl("news-123");
        setName("News");
        setXPageLayout("layout1");
        clickOkButton();

        tester.assertErrorMessages("The name \'News\' is already used in this folder. Please use a different name.");
    }

    @Test
    public void addFolderWithExistedUriNameAndLocalizedName() throws Exception {
        createDialog(false, xPageLayoutList);

        // disable the translation from Name to uriName
        clickUrlActionLink();
        setUrl("news");
        setName("News");
        setXPageLayout("layout1");
        clickOkButton();

        tester.assertErrorMessages("The URL name \'news\' and name \'News\' are already used in this folder. Please use different names.");
    }

    @Test
    public void addFolderWithWorkflowException() throws Exception {
        createDialog(true, xPageLayoutList);

        // disable the translation from Name to uriName
        clickUrlActionLink();
        setUrl("another-news");
        setName("Another News");
        setXPageLayout("layout1");
        clickOkButton();

        tester.assertErrorMessages("Failed to validate input names");
    }
}
