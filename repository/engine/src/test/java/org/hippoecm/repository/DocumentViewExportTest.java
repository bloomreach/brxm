package org.hippoecm.repository;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class DocumentViewExportTest extends RepositoryTestCase {
    
    String[] content = new String[] {
        "/test",              "nt:unstructured",
        "jcr:mixinTypes",     "mix:referenceable",
        "/test/fs",           "hippo:facetselect",
        "hippo:docbase",      "/test",
        "hippo:values",       null,
        "hippo:facets",       null,
        "hippo:modes",        null
    };

    @After
    @Override
    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        super.tearDown();
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
    
    @Test
    public void testDocumentViewExportDoesNotRecurseInVirtualLayers() throws Exception {
        build(session, content);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        session.exportDocumentView("/test", out, false, false);
    }

}
