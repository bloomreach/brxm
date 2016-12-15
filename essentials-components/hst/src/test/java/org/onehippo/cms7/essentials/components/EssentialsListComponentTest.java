/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.AbstractHstQueryTest;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onehippo.cms7.essentials.components.ext.DoBeforeRenderExtension;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EssentialsListComponentTest extends AbstractHstQueryTest {

    private static final String ROOT_FOLDER_PATH = "/content/documents/myhippoproject";
    private static final String NEWS_FOLDER_PATH = "/content/documents/myhippoproject/news";
    private static final String CONTENT_FOLDER_PATH = "/content/documents/myhippoproject/content";
    private static final int NR_NEWS_ITEMS = 3;
    private static final int NR_MY_NEWS_ITEMS = 1;
    private static final int TOTAL_NEWS_ITEMS = NR_NEWS_ITEMS + NR_MY_NEWS_ITEMS;
    private static final int NR_CONTENT_ITEMS = 2;

    private static final String MYHIPPOPROJECT_NEWSDOCUMENT = "myhippoproject:newsdocument";
    private static final String MYHIPPOPROJECT_CONTENTDOCUMENT = "myhippoproject:contentdocument";

    ObjectConverter objectConverter;
    ObjectBeanManager obm;

    @Mock
    ComponentManager componentManager;

    @Mock
    EssentialsListComponentInfo essentialsListComponentInfo;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectConverter = getObjectConverter();
        obm = new ObjectBeanManagerImpl(session, objectConverter);

        when(essentialsListComponentInfo.getIncludeSubtypes()).thenReturn(TRUE);
        when(componentManager.getComponent(DoBeforeRenderExtension.class.getName())).thenReturn(null);
        HstServices.setComponentManager(componentManager);
    }

    @After
    public void tearDown() throws Exception {
        HstServices.setComponentManager(null);
        super.tearDown();
    }

    @Test
    public void testBuildHstQuery_News() throws ObjectBeanManagerException, QueryException {
        when(essentialsListComponentInfo.getDocumentTypes()).thenReturn(MYHIPPOPROJECT_NEWSDOCUMENT);
        when(essentialsListComponentInfo.getPath()).thenReturn(NEWS_FOLDER_PATH);

        HstQueryResult hstQueryResult = executeHstQuery();

        assertEquals(TOTAL_NEWS_ITEMS, hstQueryResult.getSize());
    }

    @Test
    public void testBuildHstQuery_News_NoSubtypes() throws ObjectBeanManagerException, QueryException {
        when(essentialsListComponentInfo.getDocumentTypes()).thenReturn(MYHIPPOPROJECT_NEWSDOCUMENT);
        when(essentialsListComponentInfo.getPath()).thenReturn(NEWS_FOLDER_PATH);
        when(essentialsListComponentInfo.getIncludeSubtypes()).thenReturn(FALSE);

        HstQueryResult hstQueryResult = executeHstQuery();

        assertEquals(NR_NEWS_ITEMS, hstQueryResult.getSize());
    }

    @Test
    public void testBuildHstQuery_NewsFromRoot() throws ObjectBeanManagerException, QueryException {
        when(essentialsListComponentInfo.getDocumentTypes()).thenReturn(MYHIPPOPROJECT_NEWSDOCUMENT);
        when(essentialsListComponentInfo.getPath()).thenReturn(ROOT_FOLDER_PATH);

        HstQueryResult hstQueryResult = executeHstQuery();

        assertEquals(TOTAL_NEWS_ITEMS, hstQueryResult.getSize());
    }

    @Test
    public void testBuildHstQuery_Content() throws ObjectBeanManagerException, QueryException {
        when(essentialsListComponentInfo.getDocumentTypes()).thenReturn(MYHIPPOPROJECT_CONTENTDOCUMENT);
        when(essentialsListComponentInfo.getPath()).thenReturn(CONTENT_FOLDER_PATH);

        HstQueryResult hstQueryResult = executeHstQuery();

        assertEquals(NR_CONTENT_ITEMS, hstQueryResult.getSize());
    }

    @Test
    public void testBuildHstQuery_ContentAndNewsFromRoot() throws ObjectBeanManagerException, QueryException {
        when(essentialsListComponentInfo.getDocumentTypes()).thenReturn(MYHIPPOPROJECT_NEWSDOCUMENT + "," + MYHIPPOPROJECT_CONTENTDOCUMENT);
        when(essentialsListComponentInfo.getPath()).thenReturn(ROOT_FOLDER_PATH);

        HstQueryResult hstQueryResult = executeHstQuery();

        assertEquals(NR_CONTENT_ITEMS + TOTAL_NEWS_ITEMS, hstQueryResult.getSize());
    }

    private HstQueryResult executeHstQuery() throws ObjectBeanManagerException, QueryException {

        HippoFolder scope = (HippoFolder) obm.getObject(essentialsListComponentInfo.getPath());
        EssentialsListComponent essentialsListComponent = new EssentialsListComponent();
        MockHstRequest hstRequest = new MockHstRequest();

        HstQuery hstQuery = essentialsListComponent.buildQuery(hstRequest, essentialsListComponentInfo, scope);
        HstQueryResult hstQueryResult = hstQuery.execute();

        assertTrue(hstQuery.getQueryAsString(true) != null);

        return hstQueryResult;
    }
}
