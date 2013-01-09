/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
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

            IPluginConfig config = new JavaPluginConfig();
            config.put("units", new String[] { "center", "top" });
            config.put("top", "id=top,body=top-body");
            config.put("center", "id=center-wrapper");
            config.put("linked.with.parent", false);
            wfSettings = new WireframeSettings(config);
            add(new WireframeBehavior(wfSettings));

            Label panel;
            add(panel = new TestLabel("label", "label1", new Model("test")));
            panel.add(new UnitBehavior("center"));

            add(new Link("action") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    Label label = new TestLabel("label", "label2", new Model("testing 1 2 3"));
                    label.add(new UnitBehavior("center"));
                    TestPage.this.replace(label);
                }

            });
        }

        WireframeSettings getWireframeSettings() {
            return wfSettings;
        }
    }

    @Test
    public void testWireFrameSettings() {
        WicketTester tester = new WicketTester();
        TestPage page = (TestPage) tester.startPage(TestPage.class);
        WireframeSettings wfSettings = page.getWireframeSettings();

        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());
        JSONObject wfObject = JSONObject.fromObject(wfSettings, jsonConfig);
        assertTrue(((JSONObject) wfObject.get("rootId")).isNullObject());

        JSONArray units = wfObject.getJSONArray("units");
        Map<String, JSONObject> unitMap = new TreeMap<String, JSONObject>();
        for (int i = 0; i < units.size(); i++) {
            JSONObject unitObject = units.getJSONObject(i);
            unitMap.put((String) unitObject.get("position"), unitObject);
        }

        JSONObject centerObject = unitMap.get("center");
        assertNotNull(centerObject);
        assertTrue("center".equals(centerObject.get("position")));
        assertTrue("id01:center-wrapper".equals(centerObject.get("id")));
        assertTrue("label1".equals(centerObject.get("body")));
        assertTrue(Boolean.FALSE.equals(centerObject.get("scroll")));

        JSONObject topObject = unitMap.get("top");
        assertTrue("id01:top".equals(topObject.get("id")));
        assertTrue("id01:top-body".equals(topObject.get("body")));
    }

    @Test
    public void testUnitReplacement() {
        WicketTester tester = new WicketTester();
        tester.startPage(TestPage.class);
        tester.clickLink("action");

        TestPage page = (TestPage) tester.getLastRenderedPage();
        WireframeSettings wfSettings = page.getWireframeSettings();

        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());
        JSONObject wfObject = JSONObject.fromObject(wfSettings, jsonConfig);
        assertTrue(((JSONObject) wfObject.get("rootId")).isNullObject());

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
        assertTrue("label2".equals(centerObject.get("body")));
    }

}
