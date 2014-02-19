package org.onehippo.cms7.essentials.rest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.rest.model.RestList;
import org.onehippo.cms7.essentials.rest.model.contentblocks.CBPayload;
import org.onehippo.cms7.essentials.rest.model.contentblocks.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class ContentBlocksTest {

    private static final Logger log = LoggerFactory.getLogger(ContentBlocksTest.class);

    @Test
    public void testUnmarshalling() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CBPayload p = new CBPayload();
        final RestList<DocumentType> documentTypes = new RestList<>();
        final DocumentType resource = new DocumentType();
        documentTypes.add(resource);
        resource.setValue("foo");
        final RestList<KeyValueRestful> restfulRestList = new RestList<>();
        final KeyValueRestful keyVal = new KeyValueRestful("key", "val");
        restfulRestList.add(keyVal);
        resource.setProviders(restfulRestList);
        p.setDocumentTypes(documentTypes);
        //
        final String myPayload = mapper.writeValueAsString(p);
        log.info("myPayload {}", myPayload);
        final InputStream resourceAsStream = getClass().getResourceAsStream("/contentblocks-compare.json");
        String myString = IOUtils.toString(resourceAsStream, "UTF-8");
        //log.info("myString {}", myString);
        final CBPayload o = mapper.readValue(myString, CBPayload.class);
        final int size = o.getDocumentTypes().getItems().size();
        assertTrue(size == 2);
        CBPayload cbPayload = new CBPayload();
        final RestList<DocumentType> types = new RestList<>();
        final RestList<KeyValueRestful> keyValueRestfulRestfulList = new RestList<>();
        keyValueRestfulRestfulList.add(new KeyValueRestful("Provider 1", "Provider 1"));
        keyValueRestfulRestfulList.add(new KeyValueRestful("Provider 2", "Provider 2"));
        types.add(new DocumentType("News document", "namespace:news", keyValueRestfulRestfulList));
        types.add(new DocumentType("News documen2", "namespace:news", keyValueRestfulRestfulList));
        cbPayload.setDocumentTypes(types);
        String jsonOutput = mapper.writeValueAsString(cbPayload);
        assertTrue(StringUtils.isNotEmpty(jsonOutput));
        log.info("jsonOutput {}", jsonOutput);
        final CBPayload payload = mapper.readValue(jsonOutput, CBPayload.class);
        assertEquals(2, payload.getDocumentTypes().getItems().size());

    }

    @Test
    public void testInjection() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "testname");
        final String s = TemplateUtils.injectTemplate("template.xml", map, getClass());
        assertTrue(s.contains("testname"));
    }
}
