package org.onehippo.cms7.essentials.dashboard.utils;

import javax.jcr.Node;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class DocumentTypeUtilsTest extends BaseRepositoryTest{

    private static Logger log = LoggerFactory.getLogger(DocumentTypeUtilsTest.class);

    @Test
    public void testRegisterDocumentType() throws Exception {
        final Node node = DocumentTypeUtils.registerDocumentType("newsdocument", "basedocument", "test", getContext());
        assertEquals(null, node);
    }
}
