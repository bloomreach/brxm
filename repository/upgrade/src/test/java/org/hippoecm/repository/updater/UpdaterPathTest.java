/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.updater;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdaterPathTest {

    static void assertEqualSign(int a, int b) {
        assertTrue((a < 0 && b < 0) || (a > 0 && b > 0) || (a == 0 && b ==0)); 
    }
    
    @Test
    public void testSns() {
        assertEqualSign("/a".compareTo("/b"), new UpdaterPath("/a").compareTo(new UpdaterPath("/b")));
        assertEqualSign("/b".compareTo("/a"), new UpdaterPath("/b").compareTo(new UpdaterPath("/a")));
        assertEqualSign(-1, new UpdaterPath("/a[2]").compareTo(new UpdaterPath("/a[10]")));
        assertEqualSign("/a".compareTo("/a/b"), new UpdaterPath("/a").compareTo(new UpdaterPath("/a/b")));
        assertEqualSign("/a/b".compareTo("/a"), new UpdaterPath("/a/b").compareTo(new UpdaterPath("/a")));
        assertEqualSign("/a/b".compareTo("/a[2]"), new UpdaterPath("/a/b").compareTo(new UpdaterPath("/a[2]")));
    }

}
