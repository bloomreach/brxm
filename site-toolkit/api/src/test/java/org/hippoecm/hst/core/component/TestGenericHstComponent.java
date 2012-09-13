/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.component;

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Before;
import org.junit.Test;

public class TestGenericHstComponent {

    private ServletContext servletContext;

    @Before
    public void setUp() throws Exception {
        servletContext = EasyMock.createNiceMock(ServletContext.class);
        EasyMock.replay(servletContext);
    }

    @Test
    public void testDefaultBehaviorWhenResourcePathSet() throws Exception {
        ComponentConfiguration componentConfig = EasyMock.createNiceMock(ComponentConfiguration.class);
        EasyMock.expect(componentConfig.getServeResourcePath()).andReturn("/WEB-INF/resourcepath.jsp").anyTimes();
        EasyMock.replay(componentConfig);

        final String resourceID = "/WEB-INF/test.jsp";

        ContainerConfiguration containerConfiguration = EasyMock.createNiceMock(ContainerConfiguration.class);
        EasyMock.expect(containerConfiguration.getBoolean(GenericHstComponent.RESOURCE_PATH_BY_RESOURCE_ID, false)).andReturn(true).anyTimes();
        EasyMock.replay(containerConfiguration);

        HstRequestContext requestContext = EasyMock.createNiceMock(HstRequestContext.class);
        EasyMock.expect(requestContext.getContainerConfiguration()).andReturn(containerConfiguration).anyTimes();
        EasyMock.replay(requestContext);

        HstRequest request = EasyMock.createNiceMock(HstRequest.class);
        EasyMock.expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        EasyMock.expect(request.getResourceID()).andReturn(resourceID).anyTimes();
        EasyMock.replay(request);

        HstResponse response = EasyMock.createNiceMock(HstResponse.class);
        response.setServeResourcePath(resourceID);
        EasyMock.expectLastCall().andThrow(
                new AssertionError("HstResponse.setServeResourcePath() must not be called because component configuration already has serveResourcePath config."));
        EasyMock.replay(response);

        GenericHstComponent component = new GenericHstComponent();
        component.init(servletContext, componentConfig);
        component.doBeforeServeResource(request, response);
    }

    @Test
    public void testDefaultBehaviorWhenSettingResourceID() throws Exception {
        ComponentConfiguration componentConfig = EasyMock.createNiceMock(ComponentConfiguration.class);
        EasyMock.expect(componentConfig.getServeResourcePath()).andReturn(null).anyTimes();
        EasyMock.replay(componentConfig);

        final String resourceID = "my-custom-ajax-action";

        ContainerConfiguration containerConfiguration = EasyMock.createNiceMock(ContainerConfiguration.class);
        EasyMock.expect(containerConfiguration.getBoolean(GenericHstComponent.RESOURCE_PATH_BY_RESOURCE_ID, false)).andReturn(true).anyTimes();
        EasyMock.replay(containerConfiguration);

        HstRequestContext requestContext = EasyMock.createNiceMock(HstRequestContext.class);
        EasyMock.expect(requestContext.getContainerConfiguration()).andReturn(containerConfiguration).anyTimes();
        EasyMock.replay(requestContext);

        HstRequest request = EasyMock.createNiceMock(HstRequest.class);
        EasyMock.expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        EasyMock.expect(request.getResourceID()).andReturn(resourceID).anyTimes();
        EasyMock.replay(request);

        HstResponse response = EasyMock.createNiceMock(HstResponse.class);
        response.setServeResourcePath(resourceID);
        EasyMock.expectLastCall().andThrow(
                new AssertionError("HstResponse.setServeResourcePath() must not be called because component configuration already has serveResourcePath config."));
        EasyMock.replay(response);

        // when you're using a custom resourceID for ajax programming, you're responsible for dealing with it by yourself by overriding #doBeforeServeResource(...)
        GenericHstComponent component = new GenericHstComponent() {
            public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
                if ("my-custom-ajax-action".equals(request.getResourceID())) {
                    // set serveResourcePath for this ajax action...
                }
 
                return;
            }
        };

        component.init(servletContext, componentConfig);
        component.doBeforeServeResource(request, response);
    }

    @Test
    public void testBehaviorWhenSettingResourceIDAllowed() throws Exception {
        ComponentConfiguration componentConfig = EasyMock.createNiceMock(ComponentConfiguration.class);
        EasyMock.expect(componentConfig.getServeResourcePath()).andReturn(null).anyTimes();
        EasyMock.replay(componentConfig);

        final String resourceID = "/WEB-INF/test.ftl";

        ContainerConfiguration containerConfiguration = EasyMock.createNiceMock(ContainerConfiguration.class);
        EasyMock.expect(containerConfiguration.getBoolean(GenericHstComponent.RESOURCE_PATH_BY_RESOURCE_ID, false)).andReturn(true).anyTimes();
        EasyMock.replay(containerConfiguration);

        HstRequestContext requestContext = EasyMock.createNiceMock(HstRequestContext.class);
        EasyMock.expect(requestContext.getContainerConfiguration()).andReturn(containerConfiguration).anyTimes();
        EasyMock.replay(requestContext);

        HstRequest request = EasyMock.createNiceMock(HstRequest.class);
        EasyMock.expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        EasyMock.expect(request.getResourceID()).andReturn(resourceID).anyTimes();
        EasyMock.replay(request);

        HstResponse response = EasyMock.createNiceMock(HstResponse.class);
        response.setServeResourcePath(resourceID);
        EasyMock.expectLastCall().andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                String serveResourcePath = (String) EasyMock.getCurrentArguments()[0];
                Assert.assertEquals(resourceID, serveResourcePath);
                return serveResourcePath;
            }
        });
        EasyMock.replay(response);

        GenericHstComponent component = new GenericHstComponent();
        component.init(servletContext, componentConfig);
        component.doBeforeServeResource(request, response);
    }

    @Test
    public void testBehaviorWithInvalidResourceID() throws Exception {
        ComponentConfiguration componentConfig = EasyMock.createNiceMock(ComponentConfiguration.class);
        EasyMock.expect(componentConfig.getServeResourcePath()).andReturn(null).anyTimes();
        EasyMock.replay(componentConfig);

        final String resourceID = "/WEB-INF/hst-config.properties";

        ContainerConfiguration containerConfiguration = EasyMock.createNiceMock(ContainerConfiguration.class);
        EasyMock.expect(containerConfiguration.getBoolean(GenericHstComponent.RESOURCE_PATH_BY_RESOURCE_ID, false)).andReturn(true).anyTimes();
        EasyMock.replay(containerConfiguration);

        HstRequestContext requestContext = EasyMock.createNiceMock(HstRequestContext.class);
        EasyMock.expect(requestContext.getContainerConfiguration()).andReturn(containerConfiguration).anyTimes();
        EasyMock.replay(requestContext);

        HstRequest request = EasyMock.createNiceMock(HstRequest.class);
        EasyMock.expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        EasyMock.expect(request.getResourceID()).andReturn(resourceID).anyTimes();
        EasyMock.replay(request);

        HstResponse response = EasyMock.createNiceMock(HstResponse.class);
        response.sendError(eq(HstResponse.SC_NOT_FOUND));
        EasyMock.expectLastCall();

        EasyMock.replay(response);

        GenericHstComponent component = new GenericHstComponent();
        component.init(servletContext, componentConfig);

        try {
            component.doBeforeServeResource(request, response);
            EasyMock.verify(response);
        } catch (HstComponentException e) {
            fail("Should not get a HstComponentException");
        }
    }
}
