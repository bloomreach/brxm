/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.gotolink;

import org.apache.wicket.IClusterable;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.TestPanelSource;
import org.apache.wicket.util.tester.WicketTester;

import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IRenderService;

import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Test case for <code>GotolinkDocumentsShortcutPlugin</code>,
 * the test makes use of the WicketTester
 *
 * @author Jettro Coenradie
 */
public class GotolinkDocumentsShortcutPluginTest {

    final IBrowseService mockBrowseService = createMock("browseService", IBrowseService.class);
    final IModelReference mockModelReference = createMock("modelReference", IModelReference.class);
    final IModel mockModel = createMock("model", IModel.class);
    final IPluginContext mockPluginContext = createMock("mockPluginContext", IPluginContext.class);
    final IRenderService mockRenderService = createMock("mockRenderService", IRenderService.class);

    private static final String WICKET_MODEL = "model.browse.folder";
    private static final String WICKET_ID = "shortcut";

    @Test
    public void checkClickedAjaxLink() {
        mockPluginContext.registerService(isA(IClusterable.class), isA(String.class));
        mockPluginContext.registerService(isA(MarkupContainer.class), isA(String.class));

        expect(mockPluginContext.getService(WICKET_MODEL, IModelReference.class)).andReturn(mockModelReference).times(2);
        expect(mockPluginContext.getService("dummy.browserid", IBrowseService.class)).andReturn(mockBrowseService);
        expect(mockPluginContext.getService("dummy.browserid", IRenderService.class)).andReturn(mockRenderService);

        expect(mockModelReference.getModel()).andReturn(mockModel);
        mockModel.detach();
        expectLastCall().times(2);

        mockBrowseService.browse(isA(JcrNodeModel.class));
        mockRenderService.focus(null);

        replay(mockBrowseService, mockRenderService, mockModelReference, mockModel, mockPluginContext);

        WicketTester tester = new WicketTester(new HippoWebApplication());

        TestPanelSource testPanelSource = createTestPanelSource();
        tester.startPanel(testPanelSource);
        tester.clickLink("panel:link", true); // true = is an ajax link
        verify(mockBrowseService, mockRenderService, mockModelReference, mockModel, mockPluginContext);
    }

    private TestPanelSource createTestPanelSource() {
        return new TestPanelSource() {
            public Panel getTestPanel(String panelId) {
                IPluginConfig iPluginConfig = new JavaPluginConfig(panelId);
                iPluginConfig.put("browser.id", "dummy.browserid");
                iPluginConfig.put("wicket.model", WICKET_MODEL);
                iPluginConfig.put("wicket.id", WICKET_ID);

                GotolinkDocumentsShortcutPlugin documentsShortcutPlugin = new GotolinkDocumentsShortcutPlugin(mockPluginContext,
                                                                                                              iPluginConfig);
                // This is just used to change the id of the plugin
                documentsShortcutPlugin.bind(documentsShortcutPlugin, panelId);

                return documentsShortcutPlugin;
            }
        };
    }

    public class HippoWebApplication extends WicketTester.DummyWebApplication {
        @Override
        public Class getHomePage() {
            return MyHomePage.class;
        }
    }
}
