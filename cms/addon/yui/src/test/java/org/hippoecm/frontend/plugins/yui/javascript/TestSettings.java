/*
 *  Copyright 2009 Hippo.
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

import junit.framework.TestCase;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.wicket.behavior.IBehavior;
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
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.junit.Test;

public class TestSettings extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
    public void testUnitSettingsInitializationFromConfig() {
        IPluginConfig config = new JavaPluginConfig();
        config.put("position", "top");
        config.put("scroll", Boolean.TRUE);

        UnitSettings unitSettings = new UnitSettings(config);
        assertTrue("top".equals(unitSettings.getPosition()));

        JSONObject wfObject = JSONObject.fromObject(unitSettings.toScript());
        assertTrue("top".equals(wfObject.get("position")));
        assertTrue(Boolean.TRUE.equals(wfObject.get("scroll")));
    }

    @Test
    public void testUnitSettingsInitializationFromString() {
        UnitSettings unitSettings = new UnitSettings("top", new ValueMap("scroll=true"));
        assertTrue("top".equals(unitSettings.getPosition()));
        JSONObject wfObject = JSONObject.fromObject(unitSettings.toScript());
        assertTrue("top".equals(wfObject.get("position")));
        assertTrue(Boolean.TRUE.equals(wfObject.get("scroll")));
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
            config.put("wrappers", new String[] { "center" });
            config.put("center", "center-wrapper");
            wfSettings = new WireframeSettings(config);
            add(new WireframeBehavior(wfSettings));

            Label panel;
            add(panel = new TestLabel("label", "label1", new Model("test")));
            panel.add(new UnitBehavior(new UnitSettings("center", new ValueMap(""))));

            add(new Link("action") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    Label label = new TestLabel("label", "label2", new Model("testing 1 2 3"));
                    label.add(new UnitBehavior(new UnitSettings("center", new ValueMap("scroll=true"))));
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

        System.out.println(wfSettings.toScript());
        JSONObject wfObject = JSONObject.fromObject(wfSettings.toScript());
        assertTrue("".equals(wfObject.get("rootId")));

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
        assertTrue("center".equals(centerObject.get("position")));
        assertTrue("id01:center-wrapper".equals(centerObject.get("id")));
        assertTrue("label1".equals(centerObject.get("body")));
        assertTrue(Boolean.FALSE.equals(centerObject.get("scroll")));
    }

    @Test
    public void testUnitReplacement() {
        WicketTester tester = new WicketTester();
        tester.startPage(TestPage.class);
        tester.clickLink("action");

        TestPage page = (TestPage) tester.getLastRenderedPage();
        WireframeSettings wfSettings = page.getWireframeSettings();

        JSONObject wfObject = JSONObject.fromObject(wfSettings.toScript());
        assertTrue("".equals(wfObject.get("rootId")));

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
