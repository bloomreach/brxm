/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReferringDocumentsProviderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testReferringDocumentsStatementDepthAtLeast1() {
         ReferringDocumentsProvider.createReferrersStatement(false, "cafebabe-cafe-babe-cafe-babecafebabe", 0);
    }

    @Test
    public void testReferringDocumentsStatement() {
        final String referrersStatement1 = ReferringDocumentsProvider.createReferrersStatement(false, "cafebabe-cafe-babe-cafe-babecafebabe", 1);
        assertEquals("//element(*,hippo:handle)[*/hippo:availability='live' and (*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe')] order by @jcr:name ascending",
                referrersStatement1);
        final String referrersStatement2 = ReferringDocumentsProvider.createReferrersStatement(false, "cafebabe-cafe-babe-cafe-babecafebabe", 2);
        assertEquals("//element(*,hippo:handle)[*/hippo:availability='live' and (*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe' or */*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe')] order by @jcr:name ascending",
                referrersStatement2);

        final String referrersStatement3 = ReferringDocumentsProvider.createReferrersStatement(false, "cafebabe-cafe-babe-cafe-babecafebabe", 3);
        assertEquals("//element(*,hippo:handle)[*/hippo:availability='live' and (*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe' or */*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe' or */*/*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe')] order by @jcr:name ascending",
                referrersStatement3);
    }
}
