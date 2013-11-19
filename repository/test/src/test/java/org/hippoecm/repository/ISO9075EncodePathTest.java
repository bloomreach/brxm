/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import org.apache.jackrabbit.util.ISO9075;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ISO9075EncodePathTest {

    @Test
    public void testEncodePaths() {
        assertEquals("/jcr:root/foo/bar", encodeXpath("/jcr:root/foo/bar"));
        assertEquals("/jcr:root/foo/_x0037_8", encodeXpath("/jcr:root/foo/78"));
        assertEquals("/jcr:root/foo/bar[@my:project = '456']", encodeXpath("/jcr:root/foo/bar[@my:project = '456']"));
        assertEquals("/jcr:root/foo/_x0037_8[@my:project = '456']", encodeXpath("/jcr:root/foo/78[@my:project = '456']"));
        assertEquals("/jcr:root/foo/_x0037_8[test/@my:project = '456']", encodeXpath("/jcr:root/foo/78[test/@my:project = '456']"));

        // we do not encode *IN* where clauses
        assertEquals("/jcr:root/foo/_x0037_8[99/@my:project = '456']", encodeXpath("/jcr:root/foo/78[99/@my:project = '456']"));


        assertEquals("//element(*,hippo:document)", encodeXpath("//element(*,hippo:document)"));
        assertEquals("/jcr:root/foo/bar//element(*,hippo:document)", encodeXpath("/jcr:root/foo/bar//element(*,hippo:document)"));
        assertEquals("/jcr:root/foo/_x0037_8//element(*,hippo:document)",  encodeXpath("/jcr:root/foo/78//element(*,hippo:document)"));


        assertEquals("/jcr:root/foo/_x0037_8//element(*,hippo:document)[@my:project = 'test']",
                encodeXpath("/jcr:root/foo/78//element(*,hippo:document)[@my:project = 'test']"));

        // we do not encode *IN* where clauses
        assertEquals("/jcr:root/foo/_x0037_8//element(*,hippo:document)[99/@my:project = 'test']",
                encodeXpath("/jcr:root/foo/78//element(*,hippo:document)[99/@my:project = 'test']"));


        assertEquals("//*", encodeXpath("//*"));
        assertEquals("//*[jcr:contains(.,'test')]", encodeXpath("//*[jcr:contains(.,'test')]"));
        assertEquals("//element(*,hippo:document)", encodeXpath("//element(*,hippo:document)"));

    }
    
    
    private static String encodeXpath(String xpath) {
        final int whereClauseIndexStart = xpath.indexOf("[");
        final int whereClauseIndexEnd =xpath.lastIndexOf("]");
        if (whereClauseIndexStart > -1 && whereClauseIndexEnd > -1) {
            String beforeWhere = xpath.substring(0, whereClauseIndexStart);
            String afterWhere = xpath.substring(whereClauseIndexEnd + 1, xpath.length());
            // in where clause we can have path constraints
            String whereClause = "[" + xpath.substring(whereClauseIndexStart + 1, whereClauseIndexEnd) + "]";
            return encodePathConstraint(beforeWhere) + whereClause + afterWhere;
        } else if (whereClauseIndexStart == -1 && whereClauseIndexEnd == -1) {
            // only path
            return encodePathConstraint(xpath);
        } else {
            // most likely incorrect query
            return xpath;
        }

    }

    private static String encodePathConstraint(final String path) {
        String[] segments = path.split("/");
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment.startsWith("element(")) {
                builder.append(segment);
            } else if (segment.equals("*")) {
                builder.append(segment);
            } else if (segment.startsWith("@")) {
                builder.append(segment);
            } else {
                builder.append(ISO9075.encode(segment));
            }
            builder.append("/");
        }
        return builder.substring(0, builder.length() -1).toString();
    }
}
