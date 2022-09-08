/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.platform.configuration.channel;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ChannelManagerImplTest {

    @Test
    public void testFindSubstitutedNodeNames() {
        System.setProperty("brc.environmentname", "dev-green");

        // node name is same as system property
        testSubstitution("dev-green", "${brc.environmentname}");

        // node name with prefix
        testSubstitution("www-dev-green", "www-${brc.environmentname}");

        // node name with postfix
        testSubstitution("dev-green-www", "${brc.environmentname}-www");

        // node name with pre/postfix
        testSubstitution("my-dev-green-www", "my-${brc.environmentname}-www");
    }

    private void testSubstitution(final String hostNameSegment, final  String expectedNodeName) {
        final Set substitutedNodeNames = ChannelManagerImpl.findSubstitutedNodeNames(hostNameSegment);
        assertTrue("substitutedNodeNames should contain " + expectedNodeName + ", it is " + substitutedNodeNames,
                substitutedNodeNames.contains(expectedNodeName));
    }
}
