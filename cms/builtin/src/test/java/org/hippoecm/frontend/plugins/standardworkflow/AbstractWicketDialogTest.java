/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standardworkflow;

import java.io.IOException;
import java.util.Locale;

import javax.jcr.RepositoryException;

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
import org.junit.Before;
import org.onehippo.repository.mock.MockNode;

abstract class AbstractWicketDialogTest {

    private static final String WICKET_PATH_DIALOG_FORM = "dialog:content:form";
    private static final String WICKET_PATH_OK_BUTTON = "dialog:content:form:buttons:1:button";

    protected HippoTester tester;
    protected FormTester formTester;
    protected PluginPage home;
    protected PluginContext context;
    protected MockNode root;

    @Before
    public void setUp() throws RepositoryException, IOException {
        root = MockNode.root();

        tester = new HippoTester();
        tester.getSession().setLocale(Locale.ENGLISH);

        home = (PluginPage) tester.startPluginPage();

        final JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = home.getPluginManager().start(config);
    }

    protected WorkflowDescriptorModel createErrorWorkflow() throws RepositoryException {
        final WorkflowDescriptorModel workflowDescriptorModel = EasyMock.createMock(WorkflowDescriptorModel.class);
        EasyMock.expect(workflowDescriptorModel.getNode()).andThrow(new RepositoryException("No node found"));
        EasyMock.replay(workflowDescriptorModel);
        return workflowDescriptorModel;
    }

    protected IWorkflowInvoker mockWorkflowInvoker() throws Exception {
        final IWorkflowInvoker invoker = EasyMock.createMock(IWorkflowInvoker.class);
        invoker.invokeWorkflow();
        EasyMock.expectLastCall();
        EasyMock.replay(invoker);
        return invoker;
    }

    protected FormTester executeDialog(final IDialogService.Dialog dialog) {
        tester.runInAjax(home, () -> getDialogService().show(dialog));
        formTester = tester.newFormTester(WICKET_PATH_DIALOG_FORM);
        return formTester;
    }

    protected void clickOkButton() {
        tester.executeAjaxEvent(home.get(WICKET_PATH_OK_BUTTON), "click");
    }

    private IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }
}
