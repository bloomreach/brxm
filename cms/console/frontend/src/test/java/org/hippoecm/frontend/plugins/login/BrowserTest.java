/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.login;

import org.hippoecm.frontend.plugins.login.BrowserCheckBehavior.BrowserCheck;
import org.hippoecm.frontend.plugins.login.BrowserCheckBehavior.BrowserInfo;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserTest {

    static final TestBrowserInfo IE6 = new TestBrowserInfo("ie", 6, -1);
    static final TestBrowserInfo IE7 = new TestBrowserInfo("ie", 7, -1);
    static final TestBrowserInfo IE8 = new TestBrowserInfo("ie", 8, -1);

    static final TestBrowserInfo FF = new TestBrowserInfo("firefox", -1, -1);
    static final TestBrowserInfo FF3 = new TestBrowserInfo("firefox", 3, -1);
    static final TestBrowserInfo FF35 = new TestBrowserInfo("firefox", 3, 5);

    static final TestBrowserInfo SAFARI = new TestBrowserInfo("safari", -1, -1);
    static final TestBrowserInfo SAFARI3 = new TestBrowserInfo("safari", 3, -1);
    static final TestBrowserInfo SAFARI4 = new TestBrowserInfo("safari", 4, -1);

    static final TestBrowserInfo UNKNOWN = new TestBrowserInfo("unknown", -1, -1);

    @Test
    public void testNoUnsupported() throws Exception {
        checkFalse(new BrowserCheck(new String[] { }), IE6, IE7, IE8, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testNoVersion() throws Exception {
        BrowserCheck check = new BrowserCheck(new String[] { "ie" });
        checkTrue(check, IE6, IE7, IE8);
        checkFalse(check,FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testNoVersionMultiple() throws Exception {
        BrowserCheck check = new BrowserCheck(new String[] { "ie", "firefox" });
        checkTrue(check, IE6, IE7, IE8, FF, FF3, FF35);
        checkFalse(check, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testVersionMajor() throws Exception {
        BrowserCheck check = new BrowserCheck(new String[] { "ie 6" });
        checkTrue(check, IE6);
        checkFalse(check, IE7, IE8, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 7" });
        checkTrue(check, IE7);
        checkFalse(check, IE6, IE8, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testVersionMajorMinor() throws Exception {
        BrowserCheck check = new BrowserCheck(new String[] { "firefox 3.5" });
        checkTrue(check, FF35);
        checkFalse(check, IE6, IE7, IE8, FF, FF3, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testVersionMajorMultiple() throws Exception {
        BrowserCheck check = new BrowserCheck(new String[] { "ie 6", "firefox 3.5" });
        checkTrue(check, IE6, FF35);
        checkFalse(check, IE7, IE8, FF, FF3, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 7", "ie 8", "safari 4" });
        checkTrue(check, IE7, IE8, SAFARI4);
        checkFalse(check, IE6, FF, FF3, FF35, SAFARI, SAFARI3, UNKNOWN);
    }

    @Test
    public void testVersionAndModifier() throws Exception {
        BrowserCheck check = new BrowserCheck(new String[] { "ie 6 =" });
        checkTrue(check, IE6);
        checkFalse(check, IE7, IE8, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 6 >" });
        checkTrue(check, IE7, IE8);
        checkFalse(check, IE6, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 6 >=" });
        checkTrue(check, IE6, IE7, IE8);
        checkFalse(check, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 7 <" });
        checkTrue(check, IE6);
        checkFalse(check, IE7, IE8, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 7 <=" });
        checkTrue(check, IE6, IE7);
        checkFalse(check, IE8, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testVersionAndModifierMultiple() throws Exception {
        BrowserCheck check = new BrowserCheck(new String[] { "ie 6 =", "firefox" });
        checkTrue(check, IE6, FF, FF3, FF35);
        checkFalse(check, IE7, IE8, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 6 >" });
        checkTrue(check, IE7, IE8);
        checkFalse(check, IE6, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 6 >=" });
        checkTrue(check, IE6, IE7, IE8);
        checkFalse(check, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 7 <" });
        checkTrue(check, IE6);
        checkFalse(check, IE7, IE8, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "ie 7 <=" });
        checkTrue(check, IE6, IE7);
        checkFalse(check, IE8, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testComplex() throws Exception {
        BrowserCheck check = new BrowserCheck(new String[] { "ie 7 <=", "firefox 3.5", "safari" });
        checkTrue(check, IE6, IE7, FF35, SAFARI, SAFARI3, SAFARI4);
        checkFalse(check, IE8, FF, FF3, UNKNOWN);

    }

    private void checkFalse(BrowserCheck check, TestBrowserInfo... infos) {
        for(TestBrowserInfo info : infos) {
            assertFalse(check.isSupported(info));
        }
    }

    private void checkTrue(BrowserCheck check, TestBrowserInfo... infos) {
        for(TestBrowserInfo info : infos) {
            assertTrue(check.isSupported(info));
        }
    }

    static class TestBrowserInfo implements BrowserInfo {

        int major = -1;
        int minor = -1;
        String agent = "";

        public TestBrowserInfo(String agent, int major, int minor) {
            this.agent = agent;
            this.minor = minor;
            this.major = major;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public boolean isChrome() {
            return agent.equals("chrome");
        }

        public boolean isFirefox() {
            return agent.equals("firefox");
        }

        public boolean isInternetExplorer() {
            return agent.equals("ie");
        }

        public boolean isOpera() {
            return agent.equals("opera");
        }

        public boolean isSafari() {
            return agent.equals("safari");
        }

        public boolean isEdge() {
            return agent.equals("edge");
        }
    }

}
