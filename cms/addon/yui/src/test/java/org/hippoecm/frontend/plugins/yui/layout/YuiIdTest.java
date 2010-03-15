package org.hippoecm.frontend.plugins.yui.layout;

import static org.junit.Assert.assertEquals;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import org.junit.Test;

public class YuiIdTest {

    public static class TestSetting {

        public YuiId getId() {
            YuiId id = new YuiId("child");
            id.setParentId("parent");
            return id;
        }
    }

    @Test
    public void testSerialization() {
        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());

        String result = JSONObject.fromObject(new TestSetting(), jsonConfig).toString();
        assertEquals("{\"id\":\"parent:child\"}", result);
    }
}
