package org.onehippo.cms7.essentials.rest;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.RestList;
import org.onehippo.cms7.essentials.rest.model.contentblocks.CBPayload;
import org.onehippo.cms7.essentials.rest.model.contentblocks.DocumentType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class ContentBlocksTest {


    @Test
    public void testUnmarshalling() throws Exception {

        Type type = new TypeToken<CBPayload>() {
        }.getType();

        final InputStream resourceAsStream = getClass().getResourceAsStream("/contentblocks-compare.json");
        String myString = IOUtils.toString(resourceAsStream, "UTF-8");

        final CBPayload o = new Gson().fromJson(myString, type);
        final int size = o.getDocumentTypes().getItems().size();
        assertTrue(size==2);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        CBPayload cbPayload = new CBPayload();
        final RestfulList<DocumentType> types = new RestList<>();
        final RestfulList<KeyValueRestful> keyValueRestfulRestfulList = new RestList<>();
        keyValueRestfulRestfulList.add(new KeyValueRestful("Provider 1", "Provider 1"));
        keyValueRestfulRestfulList.add(new KeyValueRestful("Provider 2", "Provider 2"));
        types.add(new DocumentType("News document", "namespace:news", keyValueRestfulRestfulList));
        types.add(new DocumentType("News documen2", "namespace:news", keyValueRestfulRestfulList));
        cbPayload.setDocumentTypes(types);
        String jsonOutput = gson.toJson(cbPayload);
        assertTrue(StringUtils.isNotEmpty(jsonOutput));

    }

    @Test
    public void testInjection() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "testname");
        final String s = TemplateUtils.injectTemplate("template.xml", map, getClass());
        assertTrue(s.contains("testname"));
    }
}
