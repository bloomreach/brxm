package org.hippoecm.repository.jackrabbit.xml;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.api.HippoSession;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class DereferencedExportTest extends RepositoryTestCase {
    
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
        String actual = normalize(new String(out.toByteArray()));
        String expected = normalize(IOUtils.toString(getClass().getClassLoader().getResourceAsStream("export/expected.xml")));
        assertEquals(expected, actual);
    }

    private String normalize(final String s) {
        return s.replace("\n", "").replace("\r","");
    }

}
