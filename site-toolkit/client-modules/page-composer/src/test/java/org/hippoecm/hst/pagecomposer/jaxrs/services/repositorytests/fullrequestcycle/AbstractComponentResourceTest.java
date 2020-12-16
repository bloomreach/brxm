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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle;

import javax.jcr.Session;

import org.hippoecm.hst.pagecomposer.jaxrs.AbstractFullRequestCycleTest;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.testutils.RepositoryTestCase;

public abstract class AbstractComponentResourceTest extends AbstractFullRequestCycleTest {

    protected static final String PREVIEW_CONTAINER_TEST_PAGE_PATH = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/containertestpage";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Session session = backupHstAndCreateWorkspace();

        // create a container and container item  in workspace and also add a catalog item that can be put in the container
        String[] content = new String[] {
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage", "hst:containeritempackage",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem", "hst:containeritemcomponent",
                  "hst:componentclassname", "org.hippoecm.hst.test.BannerComponent",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testdefinition", "hst:componentdefinition",
                  "hst:componentclassname", "org.hippoecm.hst.test.BannerComponent",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testdynamiccomponentitem", "hst:componentdefinition",
                  "hst:componentclassname", "org.hippoecm.hst.component.support.bean.dynamic.BaseHstDynamicComponent",
                  "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testdynamiccomponentitem/dynamicParam1", "hst:dynamicparameter",
                  "hst:defaultvalue", "a default value",
                  "hst:valuetype", "text",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testdynamicmenuitem", "hst:componentdefinition",
                  "hst:componentclassname", "org.hippoecm.hst.component.support.bean.dynamic.MenuDynamicComponent",
                  "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testdynamicmenuitem/menuDynamicParam", "hst:dynamicparameter",
                  "hst:defaultvalue", "menu default value",
                  "hst:valuetype", "text",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testdynamicqueryitem", "hst:componentdefinition",
                  "hst:componentclassname", "org.hippoecm.hst.component.support.bean.dynamic.DocumentQueryDynamicComponent",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage", "hst:component",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main", "hst:component",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container", "hst:containercomponent",
                  "hst:xtype", "hst.vbox",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/banner-new-style", "hst:containeritemcomponent",
                  "hst:componentdefinition", "hst:components/testpackage/testitem",
                  "hst:parameternames", "path",
                  "hst:parametervalues", "/content/document",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/banner-old-style", "hst:containeritemcomponent",
                  "hst:componentclassname", "org.hippoecm.hst.test.BannerComponent",
                  "hst:parameternames", "path",
                  "hst:parametervalues", "/content/document",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/testcatalogmenuitem",  "hst:containeritemcomponent",
                  "hst:componentdefinition", "hst:components/testpackage/testdynamicmenuitem",
                  "hst:parameternames", "menu",
                  "hst:parameternames", "menuDynamicParam",
                  "hst:parametervalues", "main",
                  "hst:parametervalues", "value in menu",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/testcatalogqueryitem",  "hst:containeritemcomponent",
                  "hst:componentdefinition", "hst:components/testpackage/testdynamicqueryitem",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/testcatalogitem",  "hst:containeritemcomponent",
                  "hst:componentdefinition", "hst:components/testpackage/testdynamiccomponentitem",
                  "hst:parameternames", "dynamicParam1",
                  "hst:parametervalues", "value in container item",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container2", "hst:containercomponent",
                  "hst:xtype", "hst.vbox",
        };

        RepositoryTestCase.build(content, session);

        session.save();

    }

    @After
    public void tearDown() throws Exception {
        try {
            final Session session = createSession("admin", "admin");
            restoreHstConfigBackup(session);
            session.logout();
        } finally {
            super.tearDown();
        }
    }

}
