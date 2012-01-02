package org.hippoecm.repository.jackrabbit.xml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoSession;
import org.junit.Test;

public class DereferencedExportTest extends TestCase {
    
    private final static String[] content = {
        "/test", "nt:unstructured",
        "foo", "bar",
        "/test/quz", "nt:unstructured"
    };

    @Test
    public void testSimpleDereferencedExport() throws Exception {
        build(session, content);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView("/test", out, false, false);
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("export/expected.xml"), expected);
        assertEquals(new String(expected.toByteArray()), new String(out.toByteArray()));
    }

}
