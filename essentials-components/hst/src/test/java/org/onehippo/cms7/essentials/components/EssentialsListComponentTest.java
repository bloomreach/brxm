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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EssentialsListComponentTest extends AbstractHstQueryTest {


    private static final String NEWS_FOLDER_PATH = "/content/documents/myhippoproject/news";
    private static final String MYHIPPOPROJECT_NEWSDOCUMENT = "myhippoproject:newsdocument";


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

        when(essentialsListComponentInfo.getIncludeSubtypes()).thenReturn(Boolean.FALSE);
        when(componentManager.getComponent(DoBeforeRenderExtension.class.getName())).thenReturn(null);
        HstServices.setComponentManager(componentManager);
    }

    @After
    public void tearDown() throws Exception {
        HstServices.setComponentManager(null);
        super.tearDown();
    }

    @Test
    public void testBuildHstQuery() throws ObjectBeanManagerException, QueryException {
        when(essentialsListComponentInfo.getDocumentTypes()).thenReturn(MYHIPPOPROJECT_NEWSDOCUMENT);

        HippoFolder scope = (HippoFolder) obm.getObject(NEWS_FOLDER_PATH);
        EssentialsListComponent essentialsListComponent = new EssentialsListComponent();
        MockHstRequest hstRequest = new MockHstRequest();

        HstQuery hstQuery = essentialsListComponent.buildQuery(hstRequest, essentialsListComponentInfo, scope);
        HstQueryResult hstQueryResult = hstQuery.execute();

        assertTrue(hstQuery.getQueryAsString(true) != null);
        assertEquals(3, hstQueryResult.getSize());
    }
}