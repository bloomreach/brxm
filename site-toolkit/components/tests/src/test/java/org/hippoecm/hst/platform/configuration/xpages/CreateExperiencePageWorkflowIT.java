/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.xpages;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.XPagesUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_LABEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_XPAGE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_TEMPLATE;

public class CreateExperiencePageWorkflowIT extends AbstractBeanTestCase {


    private Repository repository;
    private HippoSession adminSession;
    private HippoSession author;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        adminSession = (HippoSession) repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        author = (HippoSession) repository.login(new SimpleCredentials("author", "author".toCharArray()));
    }

    @After
    public void tearDown() throws Exception {
        adminSession.logout();
        author.logout();
        super.tearDown();
    }

    @Test
    public void folder_workflow_create_new_XPage_document_as_admin() throws Exception {
        createNewDocumentAs(adminSession);
    }

    @Test
    public void folder_workflow_create_new_XPage_document_as_author() throws Exception {
        createNewDocumentAs(author);
    }

    private void createNewDocumentAs(final HippoSession userSession) throws RepositoryException, WorkflowException, RemoteException {

        final Node xpageNode = userSession.getNode("/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage1");
        final String folder = "/unittestcontent/documents/unittestproject/News";

        try {

            userSession.save();

            final Node folderNode = userSession.getNode(folder);

            final FolderWorkflow workflow = (FolderWorkflow) userSession.getWorkspace().getWorkflowManager().getWorkflow("internal", folderNode);

            JcrTemplateNode jcrTemplateNode = XPagesUtils.xpageAsJcrTemplate(xpageNode);

            final String add = workflow.add("simple", "new-document", "newDoc", jcrTemplateNode);

            final Node node = userSession.getNode(add);

            assertThat(node.hasNode(NODENAME_HST_XPAGE))
                    .as("Expected Experience hst:xpage added below document variant")
                    .isTrue();

            final Node xpage = node.getNode(NODENAME_HST_XPAGE);

            assertThat(xpage.hasNode("430df2da-3dc8-40b5-bed5-bdc44b8445c6"))
                    .as("Expected the hippo:identifier from the 'xpage1' container to be added as child")
                    .isTrue();
            assertThat(xpage.hasNode("430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner"))
                    .as("Expected the banner component item copied as prototpye")
                    .isTrue();

            final Node banner = xpage.getNode("430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
            assertThat(banner.hasProperty(GENERAL_PROPERTY_PARAMETER_NAMES)).isTrue();
            assertThat(banner.hasProperty(GENERAL_PROPERTY_PARAMETER_VALUES)).isTrue();
            assertThat(banner.hasProperty(NODETYPE_HST_TEMPLATE)).isTrue();
            assertThat(banner.hasProperty(COMPONENT_PROPERTY_LABEL)).isTrue();


            assertThat(xpage.hasNode("430df2da-3dc8-40b5-bed5-bdc44b8445c7"))
                    .as("Expected the hippo:identifier from the 'xpage1' container 2 to be added as child")
                    .isTrue();

        } finally {
            userSession.getNode("/unittestcontent/documents/unittestproject/News/newDoc").remove();
            userSession.save();
        }
    }
}
