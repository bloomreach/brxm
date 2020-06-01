/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

    private static final TestBrowserInfo CHROME = new TestBrowserInfo("chrome", 1, -1);
    private static final TestBrowserInfo CHROME2 = new TestBrowserInfo("chrome", 2, -1);
    private static final TestBrowserInfo CHROME3 = new TestBrowserInfo("chrome", 3, -1);

    private static final TestBrowserInfo FF = new TestBrowserInfo("firefox", -1, -1);
    private static final TestBrowserInfo FF3 = new TestBrowserInfo("firefox", 3, -1);
    private static final TestBrowserInfo FF35 = new TestBrowserInfo("firefox", 3, 5);

    private static final TestBrowserInfo SAFARI = new TestBrowserInfo("safari", -1, -1);
    private static final TestBrowserInfo SAFARI3 = new TestBrowserInfo("safari", 3, -1);
    private static final TestBrowserInfo SAFARI4 = new TestBrowserInfo("safari", 4, -1);

    private static final TestBrowserInfo UNKNOWN = new TestBrowserInfo("unknown", -1, -1);

    @Test
    public void testNoUnsupported() {
        checkFalse(new BrowserCheck(new String[] { }), CHROME, CHROME2, CHROME3, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testNoVersion() {
        final BrowserCheck check = new BrowserCheck(new String[] { "chrome" });
        checkTrue(check, CHROME, CHROME2, CHROME3);
        checkFalse(check,FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testNoVersionMultiple() {
        final BrowserCheck check = new BrowserCheck(new String[] { "chrome", "firefox" });
        checkTrue(check, CHROME, CHROME2, CHROME3, FF, FF3, FF35);
        checkFalse(check, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testVersionMajor() {
        BrowserCheck check = new BrowserCheck(new String[] { "chrome 1" });
        checkTrue(check, CHROME);
        checkFalse(check, CHROME2, CHROME3, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 2" });
        checkTrue(check, CHROME2);
        checkFalse(check, CHROME, CHROME3, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testVersionMajorMinor() {
        final BrowserCheck check = new BrowserCheck(new String[] { "firefox 3.5" });
        checkTrue(check, FF35);
        checkFalse(check, CHROME, CHROME2, CHROME3, FF, FF3, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testVersionMajorMultiple() {
        BrowserCheck check = new BrowserCheck(new String[] { "chrome 1", "firefox 3.5" });
        checkTrue(check, CHROME, FF35);
        checkFalse(check, CHROME2, CHROME3, FF, FF3, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 2", "chrome 3", "safari 4" });
        checkTrue(check, CHROME2, CHROME3, SAFARI4);
        checkFalse(check, CHROME, FF, FF3, FF35, SAFARI, SAFARI3, UNKNOWN);
    }

    @Test
    public void testVersionAndModifier() {
        BrowserCheck check = new BrowserCheck(new String[] { "chrome 1 =" });
        checkTrue(check, CHROME);
        checkFalse(check, CHROME2, CHROME3, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 1 >" });
        checkTrue(check, CHROME2, CHROME3);
        checkFalse(check, CHROME, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 1 >=" });
        checkTrue(check, CHROME, CHROME2, CHROME3);
        checkFalse(check, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 2 <" });
        checkTrue(check, CHROME);
        checkFalse(check, CHROME2, CHROME3, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 2 <=" });
        checkTrue(check, CHROME, CHROME2);
        checkFalse(check, CHROME3, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testVersionAndModifierMultiple() {
        BrowserCheck check = new BrowserCheck(new String[] { "chrome 1 =", "firefox" });
        checkTrue(check, CHROME, FF, FF3, FF35);
        checkFalse(check, CHROME2, CHROME3, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 1 >" });
        checkTrue(check, CHROME2, CHROME3);
        checkFalse(check, CHROME, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 1 >=" });
        checkTrue(check, CHROME, CHROME2, CHROME3);
        checkFalse(check, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 2 <" });
        checkTrue(check, CHROME);
        checkFalse(check, CHROME2, CHROME3, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);

        check = new BrowserCheck(new String[] { "chrome 2 <=" });
        checkTrue(check, CHROME, CHROME2);
        checkFalse(check, CHROME3, FF, FF3, FF35, SAFARI, SAFARI3, SAFARI4, UNKNOWN);
    }

    @Test
    public void testComplex() {
        final BrowserCheck check = new BrowserCheck(new String[] { "chrome 2 <=", "firefox 3.5", "safari" });
        checkTrue(check, CHROME, CHROME2, FF35, SAFARI, SAFARI3, SAFARI4);
        checkFalse(check, CHROME3, FF, FF3, UNKNOWN);

    }

    private void checkFalse(final BrowserCheck check, final TestBrowserInfo... infos) {
        for(final TestBrowserInfo info : infos) {
            assertFalse(check.isSupported(info));
        }
    }

    private void checkTrue(final BrowserCheck check, final TestBrowserInfo... infos) {
        for(final TestBrowserInfo info : infos) {
            assertTrue(check.isSupported(info));
        }
    }

    private static class TestBrowserInfo implements BrowserInfo {

        private final int major;
        private final int minor;
        private final String agent;

        TestBrowserInfo(final String agent, final int major, final int minor) {
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
