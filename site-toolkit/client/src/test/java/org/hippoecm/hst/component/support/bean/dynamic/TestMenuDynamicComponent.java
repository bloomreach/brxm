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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.hippoecm.hst.component.support.bean.info.dynamic.MenuDynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.jcr.pool.util.ProxyFactory;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.hst.component.support.bean.dynamic.MenuDynamicComponent.MENU_MODEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestMenuDynamicComponent {

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
    public void testMenuDynamicComponentAnnotationExtendsFromDynamicComponentInfo() {
        MenuDynamicComponent menuDynamicComponent = new MenuDynamicComponent();

        ParametersInfo parametersInfoAnnotation = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(
            menuDynamicComponent, (ComponentConfiguration) null);

        assertTrue(DynamicComponentInfo.class.isAssignableFrom(parametersInfoAnnotation.type()));
    }

    @Test
    public void test_menu_object_is_set_as_model() {
        MenuDynamicComponent menuDynamicComponent = initializeMenuDynamicComponent(true);

        HstSiteMenu menu1 = createNiceMock(HstSiteMenu.class);

        prepareMocksToRetrieveMenu(menu1);

        MockHstRequest hstRequest = new MockHstRequest();
        hstRequest.setRequestContext(requestContext);
        MockHstResponse hstResponse = new MockHstResponse();
        menuDynamicComponent.doBeforeRender(hstRequest, hstResponse);
        assertEquals(menu1, hstRequest.getModel(MENU_MODEL));
    }

    @Test
    public void test_when_the_menu_object_doesnt_exist() {
        MenuDynamicComponent menuDynamicComponent = initializeMenuDynamicComponent(true);

        // It won't fetch any menu
        prepareMocksToRetrieveMenu(null);

        MockHstRequest hstRequest = new MockHstRequest();
        hstRequest.setRequestContext(requestContext);
        MockHstResponse hstResponse = new MockHstResponse();

        List<LogEvent> logEvents = captureLogEvents("org.hippoecm.hst",
                () -> menuDynamicComponent.doBeforeRender(hstRequest, hstResponse));

        assertNull(hstRequest.getModel(MENU_MODEL));
        assertEquals(1, logEvents.size());
        assertEquals("Invalid site menu is selected within MenuDynamicComponent: menu1",
            logEvents.get(0).getMessage().getFormattedMessage());
    }

    @Test
    public void test_when_the_menu_param_is_not_configured() {
        // initialize the component without the proper configuration
        MenuDynamicComponent menuDynamicComponent = initializeMenuDynamicComponent(false);

        replay(requestContext);

        MockHstRequest hstRequest = new MockHstRequest();
        hstRequest.setRequestContext(requestContext);
        MockHstResponse hstResponse = new MockHstResponse();

        List<LogEvent> logEvents = captureLogEvents("org.hippoecm.hst",
                () -> menuDynamicComponent.doBeforeRender(hstRequest, hstResponse));

        assertNull(hstRequest.getModel(MENU_MODEL));
        assertEquals(1, logEvents.size());
        assertEquals("No site menu is selected within MenuDynamicComponent nor set as a component parameter " +
                "(menuName)",
            logEvents.get(0).getMessage().getFormattedMessage());
    }

    private void prepareMocksToRetrieveMenu(final HstSiteMenu menu) {
        HstSiteMenus hstSiteMenus = createNiceMock(HstSiteMenus.class);
        expect(requestContext.getHstSiteMenus()).andReturn(hstSiteMenus).anyTimes();
        expect(hstSiteMenus.getSiteMenu("menu1")).andReturn(menu).anyTimes();
        replay(requestContext, hstSiteMenus);
    }

    @NotNull
    private MenuDynamicComponent initializeMenuDynamicComponent(boolean configureMenu) {
        ProxyFactory factory = new ProxyFactory();
        ComponentConfiguration compConfig =
            (ComponentConfiguration) factory.createInvokerProxy((object, method, args) -> {
                if (args == null) {
                    return null;
                } else {
                    if ("menu".equals(args[0]) && configureMenu) {
                        return "menu1";
                    }
                }
                return null;
            }, new Class [] { ComponentConfiguration.class });

        MenuDynamicComponent menuDynamicComponent = new MyMenuDynamicComponent();
        menuDynamicComponent.init(servletContext, compConfig);
        return menuDynamicComponent;
    }

    private List<LogEvent> captureLogEvents(String loggerName, Runnable runnable) {
        TestAppender testAppender = new TestAppender();
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();

        config.addAppender(testAppender);
        config.getLoggerConfig(loggerName).addAppender(testAppender, null, null);

        try {
            runnable.run();
        } finally {
            config.getLoggerConfig(loggerName).removeAppender(testAppender.getName());
        }

        return testAppender.getLog();
    }

    @ParametersInfo(type = MenuDynamicComponentInfo.class)
    private static class MyMenuDynamicComponent extends MenuDynamicComponent {
        @Override
        protected MenuDynamicComponentInfo getComponentParametersInfo(final HstRequest request) {
            MenuDynamicComponentInfo componentParametersInfo = super.getComponentParametersInfo(request);
            return new MenuDynamicComponentInfo(){

                @Override
                public String getSiteMenu() {
                    return componentParametersInfo.getSiteMenu();
                }

                @Override
                public Map<String, Object> getResidualParameterValues() {
                    return Collections.EMPTY_MAP;
                }

                @Override
                public List<DynamicParameter> getDynamicComponentParameters() {
                    return Collections.EMPTY_LIST;
                }
            };
        }
    }

    class TestAppender implements Appender {
        private final List<LogEvent> log = new ArrayList();

        public List<LogEvent> getLog() {
            return new ArrayList(log);
        }

        @Override
        public void append(final LogEvent event) {
            log.add(event);
        }

        @Override
        public String getName() {
            return "TEST_APPENDER";
        }

        @Override
        public Layout<? extends Serializable> getLayout() {
            return null;
        }

        @Override
        public boolean ignoreExceptions() {
            return false;
        }

        @Override
        public ErrorHandler getHandler() {
            return null;
        }

        @Override
        public void setHandler(final ErrorHandler handler) {

        }

        @Override
        public State getState() {
            return null;
        }

        @Override
        public void initialize() {

        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public boolean isStopped() {
            return false;
        }
    }
}
