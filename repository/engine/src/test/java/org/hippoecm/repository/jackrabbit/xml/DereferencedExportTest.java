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
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("export/expected.xml"), expected);
        assertEquals(new String(expected.toByteArray()), new String(out.toByteArray()));
    }

}
