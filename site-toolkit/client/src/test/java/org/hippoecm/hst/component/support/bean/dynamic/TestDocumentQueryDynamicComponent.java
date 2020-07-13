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
package org.hippoecm.hst.component.support.bean.dynamic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.component.pagination.Pagination;
import org.hippoecm.hst.component.support.bean.info.dynamic.DocumentQueryDynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.jcr.pool.util.ProxyFactory;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.content.beans.standard.MockHippoBeanIterator;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestDocumentQueryDynamicComponent {

    private MockServletContext servletContext;
    private HstRequestContext requestContext;

    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext(new ClassPathXmlApplicationContext());

        requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getParameterInfoProxyFactory()).andReturn(new HstParameterInfoProxyFactoryImpl()).anyTimes();

        ModifiableRequestContextProvider.set(requestContext);
    }

    @After
    public void tearDown() {
        ModifiableRequestContextProvider.set(null);
    }

    @Test
    public void testQueryDynamicComponentAnnotationExtendsFromDynamicComponentInfo() {
        DocumentQueryDynamicComponent component = new DocumentQueryDynamicComponent();

        ParametersInfo parametersInfoAnnotation = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(
                component, (ComponentConfiguration) null);

        assertTrue(DynamicComponentInfo.class.isAssignableFrom(parametersInfoAnnotation.type()));
    }

    @Test
    public void test_pagination_is_set_as_models() throws QueryException {

        Map<Object,Object> parameterNamesAndValues = new HashMap<>();
        parameterNamesAndValues.put("scope", "banners");
        parameterNamesAndValues.put("includeSubtypes", "false");
        parameterNamesAndValues.put("documentTypes", "testns:testdoctype, testns:testdoctypetwo");
        parameterNamesAndValues.put("sortField", "testns:sortfield");
        parameterNamesAndValues.put("sortOrder", "ASC");
        parameterNamesAndValues.put("hidePastItems", "true");
        parameterNamesAndValues.put("hideFutureItems", "true");
        parameterNamesAndValues.put("dateField", "testns:datefield");
        parameterNamesAndValues.put("pageSize", "5");

        ProxyFactory factory = new ProxyFactory();
        ComponentConfiguration compConfig = (ComponentConfiguration) factory.createInvokerProxy(
                (o, m, args) -> args != null ? parameterNamesAndValues.get(args[0]) : null, 
                new Class[]{ComponentConfiguration.class});

        DocumentQueryDynamicComponent documentQueryDynamicComponent = new MyDocumentQueryDynamicComponent();
        documentQueryDynamicComponent.init(servletContext, compConfig);

        HippoBean siteContentBean = createNiceMock(HippoBean.class);
        HippoBean scope = createNiceMock(HippoBean.class);
        expect(siteContentBean.getBean("banners")).andReturn(scope).anyTimes();
        expect(requestContext.getSiteContentBaseBean()).andReturn(siteContentBean).anyTimes();
        Node scopeNode = createNiceMock(Node.class);
        expect(scope.getNode()).andReturn(scopeNode);

        HstQueryManager queryManager = createNiceMock(HstQueryManager.class);
        expect(requestContext.getQueryManager()).andReturn(queryManager).anyTimes();

        HstQuery query = createStrictMock(HstQuery.class);
        expect(queryManager.createQuery((Node) null, false, "testns:testdoctype", "testns:testdoctypetwo")).andReturn(query);

        //Expect these calls on the mock object with exactly these values. 
        // This is used for testing void methods, effectively validating that they are called, and with the specific argument(s).
        // Each of the following statements can be followed by a call to expectLastCall().andVoid();
        // However this is redundant in latest versions of easymock, and can be shortened, as in the code below
        query.addScopes(new Node[]{scopeNode});
        query.setLimit(5);
        query.setOffset(10);
        query.addOrderByAscending("testns:sortfield");

        Filter filter = createNiceMock(Filter.class);
        expect(query.createFilter()).andReturn(filter).times(2);
        expect(query.getFilter()).andReturn(null).once();
        expect(query.createFilter()).andReturn(filter).once();

        query.setFilter(filter);

        HstQueryResult result = createNiceMock(HstQueryResult.class);
        expect(query.execute()).andReturn(result).once();

        TextBean bean1 = new TextBean();
        CommentBean bean2 = new CommentBean();

        HippoBeanIterator beanIterator = new MockHippoBeanIterator(Arrays.asList(new HippoBean[]{bean1, bean2, new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(), new TextBean(), new CommentBean(),}));
        expect(result.getHippoBeans()).andReturn(beanIterator).once();
        expect(result.getTotalSize()).andReturn(20).once();

        replay(requestContext, siteContentBean, scope, query, queryManager, result, filter);

        MockHstRequest hstRequest = new MockHstRequest();
        hstRequest.setRequestContext(requestContext);
        hstRequest.addParameter(DocumentQueryDynamicComponent.REQUEST_PARAM_PAGE, "3");

        MockHstResponse hstResponse = new MockHstResponse();
        documentQueryDynamicComponent.doBeforeRender(hstRequest, hstResponse);


        assertEquals("There should be 1 model set on the request", 1, hstRequest.getModelsMap().size());

        Pagination<HippoBean> pagination = (Pagination<HippoBean>) hstRequest.getModelsMap().get(DocumentQueryDynamicComponent.REQUEST_ATTR_PAGINATION);
        assertNotNull("Pagination object was not found as request model", pagination);
        assertEquals("Wrong value for offset", 10, pagination.getOffset());
        assertEquals("Wrong value for pageSize", 3, pagination.getCurrent());
        assertEquals("Expected item not found in results", pagination.getItems().get(0), bean1);
        assertEquals("Expected item not found in results", pagination.getItems().get(1), bean2);


    }

    @ParametersInfo(type = DocumentQueryDynamicComponentInfo.class)
    private static class MyDocumentQueryDynamicComponent extends DocumentQueryDynamicComponent {

        @Override
        protected DocumentQueryDynamicComponentInfo getComponentParametersInfo(final HstRequest request) {
            final DocumentQueryDynamicComponentInfo info = super.getComponentParametersInfo(request);

            return new DocumentQueryDynamicComponentInfo() {

                @Override
                public Map<String, Object> getResidualParameterValues() {
                    return Collections.emptyMap();
                }

                @Override
                public List<DynamicParameter> getDynamicComponentParameters() {
                    return Collections.emptyList();
                }

                @Override
                public String getScope() {
                    return info.getScope();//  getComponentConfiguration().getParameter("scope", null);
                }

                @Override
                public Boolean getIncludeSubtypes() {
                    return info.getIncludeSubtypes();
                }

                @Override
                public String getDocumentTypes() {
                    return info.getDocumentTypes();
                }

                @Override
                public String getSortField() {
                    return info.getSortField();
                }

                @Override
                public String getSortOrder() {
                    return info.getSortOrder();
                }

                @Override
                public Boolean getHidePastItems() {
                    return info.getHidePastItems();
                }

                @Override
                public Boolean getHideFutureItems() {
                    return info.getHideFutureItems();
                }

                @Override
                public String getDateField() {
                    return info.getDateField();
                }

                @Override
                public int getPageSize() {
                    return info.getPageSize();
                }
            };
        }
    }

    @org.hippoecm.hst.content.beans.Node(jcrType = "test:textdocument")
    public static class TextBean extends HippoDocument {
    }

    @org.hippoecm.hst.content.beans.Node(jcrType = "test:comment")
    public static class CommentBean extends HippoDocument {
    }
}