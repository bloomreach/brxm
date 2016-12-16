/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import org.hippoecm.hst.AbstractHstQueryTest;
import org.hippoecm.hst.content.beans.ContentDocument;
import org.hippoecm.hst.content.beans.LinkDepthTestDocument;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TestContentBeanUtils
 */
public class TestContentBeanUtils extends AbstractHstQueryTest {
    private static final String CONTENTBEANSTEST_HIPPO_MIRROR_HIPPO_DOCBASE = "contentbeanstest:hippo_mirror/hippo:docbase";
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String ANOTHER_SAMPLE_DOCUMENT_PATH = "/content/documents/contentbeanstest/content/another-sample-document";
    private static final String CONTENT_PATH = "/content/documents/contentbeanstest/content/";

    @Test
    public void testCreateIncomingBeansQueryPrimaryMethodWithSubtypes() throws QueryException, ObjectBeanManagerException, RepositoryException {
        ObjectConverter objectConverter = getObjectConverter();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        ContentDocument bean = (ContentDocument) obm.getObject(ANOTHER_SAMPLE_DOCUMENT_PATH);
        HippoFolder scope = (HippoFolder) obm.getObject(CONTENT_PATH);

        if (bean == null) {
            throw new PathNotFoundException(ANOTHER_SAMPLE_DOCUMENT_PATH + " not found");
        }

        List<String> linkPaths = new ArrayList<>();
        linkPaths.add("contentbeanstest:hippo_mirror/hippo:docbase");
        linkPaths.add("contentbeanstest:actuallystilladocbase");

        HstQuery hstQuery = ContentBeanUtils.createIncomingBeansQuery(bean, scope, linkPaths, HippoDocument.class, true);
        HstQueryResult hstQueryResult = hstQuery.execute();

        assertTrue(hstQuery.getQueryAsString(true) != null);
        assertEquals(2, hstQueryResult.getSize());
    }

    @Test
    public void testCreateIncomingBeansQueryPrimaryMethodWithoutSubtypes() throws QueryException, ObjectBeanManagerException, RepositoryException {
        ObjectConverter objectConverter = getObjectConverter();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        ContentDocument bean = (ContentDocument) obm.getObject(ANOTHER_SAMPLE_DOCUMENT_PATH);
        HippoFolder scope = (HippoFolder) obm.getObject(CONTENT_PATH);

        if (bean == null) {
            throw new PathNotFoundException(ANOTHER_SAMPLE_DOCUMENT_PATH + " not found");
        }

        List<String> linkPaths = new ArrayList<>();
        linkPaths.add("contentbeanstest:hippo_mirror/hippo:docbase");
        linkPaths.add("contentbeanstest:actuallystilladocbase");

        HstQuery hstQuery = ContentBeanUtils.createIncomingBeansQuery(bean, scope, linkPaths, ContentDocument.class, false);
        HstQueryResult hstQueryResult = hstQuery.execute();

        assertTrue(hstQuery.getQueryAsString(true) != null);
        assertEquals(1, hstQueryResult.getSize());
    }

    @Test
    public void testCreateIncomingBeansQueryConvenienceMethodWithSingleLinkPath() throws QueryException, ObjectBeanManagerException, RepositoryException {
        ObjectConverter objectConverter = getObjectConverter();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        ContentDocument bean = (ContentDocument) obm.getObject(ANOTHER_SAMPLE_DOCUMENT_PATH);
        HippoFolder scope = (HippoFolder) obm.getObject(CONTENT_PATH);

        if (bean == null) {
            throw new PathNotFoundException(ANOTHER_SAMPLE_DOCUMENT_PATH + " not found");
        }

        HstQuery hstQuery = ContentBeanUtils.createIncomingBeansQuery(bean, scope, CONTENTBEANSTEST_HIPPO_MIRROR_HIPPO_DOCBASE, ContentDocument.class, false);
        HstQueryResult hstQueryResult = hstQuery.execute();

        assertTrue(hstQuery.getQueryAsString(true) != null);
        assertEquals(1, hstQueryResult.getSize());
    }

    @Test
    public void testCreateIncomingBeansQueryConvenienveMethodWithAssumedLinkPathDepthTest() throws QueryException, ObjectBeanManagerException, RepositoryException {
        ObjectConverter objectConverter = getObjectConverter();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        ContentDocument bean = (ContentDocument) obm.getObject(ANOTHER_SAMPLE_DOCUMENT_PATH);
        HippoFolder scope = (HippoFolder) obm.getObject(CONTENT_PATH);

        if (bean == null) {
            throw new PathNotFoundException(ANOTHER_SAMPLE_DOCUMENT_PATH + " not found");
        }

        HstQuery hstQueryFour = ContentBeanUtils.createIncomingBeansQuery(bean, scope, 4, LinkDepthTestDocument.class);
        HstQuery hstQueryThree = ContentBeanUtils.createIncomingBeansQuery(bean, scope, 3, LinkDepthTestDocument.class);
        HstQuery hstQueryTwo = ContentBeanUtils.createIncomingBeansQuery(bean, scope, 2, LinkDepthTestDocument.class);
        HstQuery hstQueryOne = ContentBeanUtils.createIncomingBeansQuery(bean, scope, 1, LinkDepthTestDocument.class);
        HstQuery hstQueryZero = ContentBeanUtils.createIncomingBeansQuery(bean, scope, 0, LinkDepthTestDocument.class);
        HstQueryResult hstQueryResultFour = hstQueryFour.execute();
        HstQueryResult hstQueryResultThree = hstQueryThree.execute();
        HstQueryResult hstQueryResultTwo = hstQueryTwo.execute();
        HstQueryResult hstQueryResultOne = hstQueryOne.execute();
        HstQueryResult hstQueryResultZero = hstQueryZero.execute();

        assertTrue(hstQueryFour.getQueryAsString(true) != null);
        assertTrue(hstQueryThree.getQueryAsString(true) != null);
        assertTrue(hstQueryTwo.getQueryAsString(true) != null);
        assertTrue(hstQueryOne.getQueryAsString(true) != null);
        assertTrue(hstQueryZero.getQueryAsString(true) != null);
        assertEquals(5, hstQueryResultFour.getSize());
        assertEquals(4, hstQueryResultThree.getSize());
        assertEquals(3, hstQueryResultTwo.getSize());
        assertEquals(2, hstQueryResultOne.getSize());
        assertEquals(1, hstQueryResultZero.getSize());
    }

    @Test
    public void testCreateIncomingBeansQueryConvenienveMethodWithAssumedLinkPathAndBeyondMaximumDepth() throws QueryException, ObjectBeanManagerException, RepositoryException {
        thrown.expect(FilterException.class);

        ObjectConverter objectConverter = getObjectConverter();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        ContentDocument bean = (ContentDocument) obm.getObject(ANOTHER_SAMPLE_DOCUMENT_PATH);
        HippoFolder scope = (HippoFolder) obm.getObject(CONTENT_PATH);

        if (bean == null) {
            throw new PathNotFoundException(ANOTHER_SAMPLE_DOCUMENT_PATH + " not found");
        }

        HstQuery hstQuery = ContentBeanUtils.createIncomingBeansQuery(bean, scope, 5, LinkDepthTestDocument.class);
    }

    @Test
    public void testCreateIncomingBeansQueryConvenienveMethodWithAssumedLinkPathAndBeyondMinimumDepth() throws QueryException, ObjectBeanManagerException, RepositoryException {
        thrown.expect(FilterException.class);

        ObjectConverter objectConverter = getObjectConverter();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        ContentDocument bean = (ContentDocument) obm.getObject(ANOTHER_SAMPLE_DOCUMENT_PATH);
        HippoFolder scope = (HippoFolder) obm.getObject(CONTENT_PATH);

        if (bean == null) {
            throw new PathNotFoundException(ANOTHER_SAMPLE_DOCUMENT_PATH + " not found");
        }

        HstQuery hstQuery = ContentBeanUtils.createIncomingBeansQuery(bean, scope, -1, LinkDepthTestDocument.class);
    }

    @Test
    public void testIsBeanType() throws Exception {
        BaseBean base = new BaseBean();
        TextBean text = new TextBean();
        NewsBean news = new NewsBean();

        assertTrue(ContentBeanUtils.isBeanType(base, BaseBean.class.getName()));
        // We do not support simple name, but only FQCN.
        assertFalse(ContentBeanUtils.isBeanType(base, BaseBean.class.getSimpleName()));
        assertTrue(ContentBeanUtils.isBeanType(text, TextBean.class.getName()));
        // We do not support simple name, but only FQCN.
        assertFalse(ContentBeanUtils.isBeanType(text, TextBean.class.getSimpleName()));
        assertTrue(ContentBeanUtils.isBeanType(news, NewsBean.class.getName()));
        // We do not support simple name, but only FQCN.
        assertFalse(ContentBeanUtils.isBeanType(news, NewsBean.class.getSimpleName()));

        // test supertypes
        assertTrue(ContentBeanUtils.isBeanType(text, BaseBean.class.getName()));
        assertTrue(ContentBeanUtils.isBeanType(news, BaseBean.class.getName()));

        assertFalse(ContentBeanUtils.isBeanType(base, TextBean.class.getName()));
        assertFalse(ContentBeanUtils.isBeanType(base, TextBean.class.getSimpleName()));
        assertFalse(ContentBeanUtils.isBeanType(base, NewsBean.class.getName()));
        assertFalse(ContentBeanUtils.isBeanType(base, NewsBean.class.getSimpleName()));
        assertFalse(ContentBeanUtils.isBeanType(text, NewsBean.class.getName()));
        assertFalse(ContentBeanUtils.isBeanType(text, NewsBean.class.getSimpleName()));

        assertFalse(ContentBeanUtils.isBeanType(news, null));
        assertFalse(ContentBeanUtils.isBeanType(null, NewsBean.class.getName()));
        assertFalse(ContentBeanUtils.isBeanType(null, NewsBean.class.getSimpleName()));
    }

    public static class BaseBean {
    }

    public static class TextBean extends BaseBean {
    }

    public static class NewsBean extends TextBean {
    }


}
