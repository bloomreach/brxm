/*
 * Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.xml;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.api.HippoSession;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
import static org.hippoecm.repository.api.ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW;
import static org.junit.Assert.assertEquals;

public class ExportImportTest extends RepositoryTestCase {
    
    private final static String[] content = {
        "/test", "nt:unstructured",
        "foo", "bar",
        "/test/quz", "nt:unstructured",
        "qux", String.valueOf((char)27) // character that causes base64 encoding of value
    };

    @Test
    public void testSimpleDereferencedExport() throws Exception {
        build(content, session);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView("/test", out, false, false);
        String actual = normalize(new String(out.toByteArray()));
        String expected = normalize(IOUtils.toString(getClass().getClassLoader().getResourceAsStream("export/expected.xml")));
        assertEquals(expected, actual);
        ((HippoSession) session).importEnhancedSystemViewXML("/",
                getClass().getClassLoader().getResourceAsStream("export/expected.xml"),
                IMPORT_UUID_COLLISION_THROW, IMPORT_REFERENCE_NOT_FOUND_THROW, null);
        out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView("/test[2]", out, false, false);
        actual = normalize(out.toString());
        assertEquals(expected, actual);
    }

    private String normalize(final String s) {
        return s.replace("\n", "").replace("\r","");
    }

}
