/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.browser;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugins.console.icons.JcrNodeIcon;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class JcrNodeIconTest extends RepositoryTestCase {

    @Test
    public void testCssClass() throws RepositoryException, WorkflowException, RemoteException {
        assertEquals(getCssClass("/"), "fa fa-circle jcrnode-default");
        assertEquals(getCssClass("/jcr:system"), "fa fa-link jcrnode-system");
        assertEquals(getCssClass("/hcm:hcm"), "fa fa-link jcrnode-system");
        assertEquals(getCssClass("/hippo:configuration"), "fa fa-cogs jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:derivatives"), "fa fa-circle jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries"), "fa fa-question-circle jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/new-document"), "fa fa-question jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/simple/hippostd:templates/new-document"), "fa fa-umbrella jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/simple/hippostd:templates/new-document/new-document"), "fa fa-file-text jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/new-folder/hippostd:templates/hippostd:folder"), "fa fa-folder-o jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/new-collection/hippostd:templates/hippostd:directory"), "fa fa-folder jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/new-namespace/hippostd:templates/namespace"), "fa fa-bullseye jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/new-type/hippostd:templates/document/hipposysedit:prototypes"), "fa fa-star-o jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/new-file-folder/hippostd:templates/asset gallery"), "fa fa-paperclip jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:queries/hippo:templates/new-image-folder/hippostd:templates/image gallery"), "fa fa-picture-o jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:workflows"), "fa fa-refresh jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:workflows/threepane/image-gallery/frontend:renderer"), "fa fa-plug jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:workflows/threepane/folder-permissions/frontend:renderer/standard/filters"), "fa fa-cog jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:users"), "fa fa-user jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:groups"), "fa fa-users jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:frontend"), "fa fa-diamond jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:modules"), "fa fa-simplybuilt jcrnode-conf");
        assertEquals(getCssClass("/hippo:configuration/hippo:update"), "fa fa-wrench jcrnode-conf");
        assertEquals(getCssClass("/hippo:log"), "fa fa-list jcrnode-log");
        assertEquals(getCssClass("/hippo:namespaces"), "fa fa-bullseye jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:namespaces/system/String"), "fa fa-file-text jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:namespaces/system/String/hipposysedit:nodetype"), "fa fa-umbrella jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:namespaces/system/String/hipposysedit:nodetype/hipposysedit:nodetype"), "fa fa-circle jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:namespaces/system/String/editor:templates"), "fa fa-file-text-o jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:namespaces/system/String/editor:templates/_default_"), "fa fa-cogs jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:namespaces/system/String/editor:templates/_default_/root"), "fa fa-plug jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:namespaces/hippo/query/hipposysedit:prototypes"), "fa fa-star-o jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:namespaces/hipposysedit/templatetype/editor:templates/_default_/rootLayout/yui.config"), "fa fa-cog jcrnode-namespaces");
        assertEquals(getCssClass("/hippo:reports"), "fa fa-pie-chart jcrnode-reports");
    }

    private String getCssClass(final String path) throws RepositoryException {
        return getCssClass(session.getNode(path));
    }

    private String getCssClass(final Node node) {
        return JcrNodeIcon.getIconCssClass(node);
    }

    @Test
    public void testNodeIsNull() {
        assertEquals(JcrNodeIcon.getIconCssClass(null), "fa fa-exclamation-circle jcrnode-default");
    }

    @Test
    public void testHstPreview() throws RepositoryException {
        MockNode hstHst = MockNode.root().addNode("hst:hst", "hst:hst");
        assertEquals(JcrNodeIcon.getIconCssClass(hstHst), "fa fa-cloud jcrnode-hst");

        MockNode hstConfigurations = hstHst.addNode("hst:configurations", "hst:configurations");
        assertEquals(JcrNodeIcon.getIconCssClass(hstConfigurations), "fa fa-cogs jcrnode-hst");

        MockNode hstConfigurationPreview = hstConfigurations.addNode("myhippoproject-preview", "hst:configuration");
        assertEquals(JcrNodeIcon.getIconCssClass(hstConfigurationPreview), "fa fa-cog jcrnode-hst-preview");

        MockNode hstConfiguration = hstConfigurations.addNode("myhippoproject", "hst:configuration");
        assertEquals(JcrNodeIcon.getIconCssClass(hstConfiguration), "fa fa-cog jcrnode-hst");
    }

}
