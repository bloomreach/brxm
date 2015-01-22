/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.servlet.utils.BinaryPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BinariesServletCustomPageFactoryTest extends AbstractTestBinariesServlet {


    private HttpServletRequest request;
    private HstRequest hstRequest;
    private HstRequestContext hstRequestContext;
    private Session session;
    private BinariesServlet binariesServlet;

    @Before
    public void setUp() {
        try {
            ServletConfig servletConfig =  getNoParamServletConfigMock();

            request = EasyMock.createNiceMock(HttpServletRequest.class);
            hstRequest = EasyMock.createNiceMock(HstRequest.class);
            hstRequestContext = EasyMock.createNiceMock(HstRequestContext.class);
            session = EasyMock.createNiceMock(Session.class);

            expect(request.getAttribute(ContainerConstants.HST_REQUEST)).andReturn(hstRequest).anyTimes();
            expect(hstRequest.getRequestContext()).andReturn(hstRequestContext).anyTimes();
            expect(hstRequestContext.getSession()).andReturn(session).anyTimes();
            expect(session.isLive()).andReturn(true).anyTimes();

            replay(servletConfig, request, hstRequest, hstRequestContext, session);

            binariesServlet = new BinariesServlet() {

                @Override
                protected BinaryPageFactory createBinaryPageFactory() {
                    return new BinaryPageFactory() {
                        @Override
                        public BinaryPage createBinaryPage(final String resourcePath, final Session session) throws RepositoryException {
                            BinaryPage unCacheablePage = new UnCacheableBinaryPage(resourcePath);
                            initBinaryPageValues(session, unCacheablePage);
                            return unCacheablePage;
                        }
                    };
                }

                @Override
                protected void initBinaryPageValues(final Session session, final BinaryPage page) throws RepositoryException {
                    page.setFileName("foo");
                }
            };

            binariesServlet.init(servletConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class UnCacheableBinaryPage extends BinaryPage {

        private UnCacheableBinaryPage(final String resourcePath) {
            super(resourcePath);
            setCacheable(false);
        }
    }

    @After
    public void tearDown() {
        binariesServlet.destroy();
    }

    @Test
    public void test_custom_BinaryPageFactory() throws Exception {
        final BinaryPage binaryPage = binariesServlet.getBinaryPage(request, "/path/to/binary");
        assertEquals("/path/to/binary", binaryPage.getResourcePath());
        assertEquals("foo", binaryPage.getFileName());
        assertFalse(binaryPage.isCacheable());
        assertTrue(binaryPage instanceof UnCacheableBinaryPage);
    }


}
