/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.javascript;

import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.yui.layout.UnitBehavior;
import org.hippoecm.frontend.plugins.yui.layout.UnitSettings;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.plugins.yui.layout.YuiId;
import org.hippoecm.frontend.plugins.yui.layout.YuiIdProcessor;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.junit.Test;

import junit.framework.TestCase;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.JSONNull;

public class TestSettings extends TestCase {

    static class SimpleSettings extends YuiObject {
        private static final long serialVersionUID = 1L;

        public SimpleSettings(YuiType type) {
            super(type);
        }

    }

    @Test
    public void testFromString() {
        StringSetting stringSetting = new StringSetting("test");
        YuiType type = new YuiType(stringSetting);

        SimpleSettings settings = new SimpleSettings(type);
        stringSetting.set("abc", settings);
        assertTrue("abc".equals(stringSetting.get(settings)));
    }

    @Test
    public void testUnitSettingsInitializationFromString() {
        UnitSettings unitSettings = new UnitSettings("top", new ValueMap("scroll=true"));
        assertTrue("top".equals(unitSettings.getPosition()));
        assertTrue(Boolean.TRUE.equals(unitSettings.isScroll()));
    }

    public static class TestLabel extends Label {
        private static final long serialVersionUID = 1L;

        private String markupId;

        public TestLabel(String id, String markupId, IModel model) {
            super(id, model);
            setOutputMarkupId(true);
            this.markupId = markupId;
        }

        @Override
        public String getMarkupId(boolean createIfDoesNotExist) {
            return markupId;
        }
    }

    public static class TestPage extends WebPage {

        private WireframeSettings wfSettings;

        public TestPage() {
            add(new WebAppBehavior(new WebAppSettings()));

            add(new WebMarkupContainer("container") {{
                IPluginConfig config = new JavaPluginConfig();
                config.put("units", new String[]{"center", "top"});
                config.put("top", "id=top,body=top-body");
                config.put("center", "id=center-wrapper");
                config.put("linked.with.parent", false);
                wfSettings = new WireframeSettings(config);
                add(new WireframeBehavior(wfSettings));

                Label panel;
                add(panel = new TestLabel("label", "label1", Model.of("test")));
                panel.add(new UnitBehavior("center"));

                add(new AjaxLink("action") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Label label = new TestLabel("label", "label2", Model.of("testing 1 2 3"));
                        label.add(new UnitBehavior("center"));
                        getParent().replace(label);
                    }

                });
            }});

        }

        WireframeSettings getWireframeSettings() {
            return wfSettings;
        }
    }

    @Test
    public void testWireFrameSettings() {
        WicketTester tester = new WicketTester();
        TestPage page = tester.startPage(TestPage.class);
        WireframeSettings wfSettings = page.getWireframeSettings();

        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());
        JSONObject wfObject = JSONObject.fromObject(wfSettings, jsonConfig);
        assertEquals(JSONNull.getInstance(), wfObject.get("rootId"));

        JSONArray units = wfObject.getJSONArray("units");
        Map<String, JSONObject> unitMap = new TreeMap<String, JSONObject>();
        for (int i = 0; i < units.size(); i++) {
            JSONObject unitObject = units.getJSONObject(i);
            unitMap.put((String) unitObject.get("position"), unitObject);
        }

        JSONObject centerObject = unitMap.get("center");
        assertNotNull(centerObject);
        assertEquals("center", centerObject.get("position"));
        assertEquals("container2:center-wrapper", centerObject.get("id"));
        assertEquals("label1", centerObject.get("body"));
        assertEquals(false, centerObject.get("scroll"));

        JSONObject topObject = unitMap.get("top");
        assertEquals("container2:top", topObject.get("id"));
        assertEquals("container2:top-body", topObject.get("body"));
    }

    @Test
    public void testUnitReplacement() {
        WicketTester tester = new WicketTester();
        tester.startPage(TestPage.class);
        tester.clickLink("container:action");

        TestPage page = (TestPage) tester.getLastRenderedPage();
        WireframeSettings wfSettings = page.getWireframeSettings();

        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());
        JSONObject wfObject = JSONObject.fromObject(wfSettings, jsonConfig);
        assertEquals(JSONNull.getInstance(), wfObject.get("rootId"));

        JSONArray units = wfObject.getJSONArray("units");
        JSONObject centerObject = null;
        for (int i = 0; i < units.size(); i++) {
            JSONObject unitObject = units.getJSONObject(i);
            if ("center".equals(unitObject.get("position"))) {
                centerObject = unitObject;
                break;
            }
        }
        assertNotNull(centerObject);
        assertEquals("label2", centerObject.get("body"));
    }

}
