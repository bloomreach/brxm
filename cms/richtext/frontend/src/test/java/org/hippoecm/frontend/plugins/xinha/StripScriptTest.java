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
package org.hippoecm.frontend.plugins.xinha;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StripScriptTest {

    @Test
    public void stripJavascriptTest() throws Exception {
        String nullValue = null;
        assertEquals(nullValue, new StripScriptModel(new Model<String>(nullValue)).getObject());

        String empty = "";
        assertEquals(empty, new StripScriptModel(new Model<String>(empty)).getObject());

        String noScript = "<html><body><p>script</p></body></html>";
        assertEquals(noScript, new StripScriptModel(new Model<String>(noScript)).getObject());

        String simple = "<html><body><script type=\"text/javascript\">alert('hello world');</script></body></html>";
        assertEquals("<html><body></body></html>", new StripScriptModel(new Model<String>(simple)).getObject());

        String noclose = "<html><body><script type=\"text/javascript\" src=\"http://example.com/\"/></body></html>";
        assertEquals("<html><body></body></html>", new StripScriptModel(new Model<String>(noclose)).getObject());

        String multiLine = "<html><body>" +
                "<p>Some text <script type=\"text/javascript\">alert('hello world');</script></p>" +
                "\n<p>\n<script>alert('hello world');</script>\n</p>" +
                "<div><script src=\"http://www.acme.com/hack.js\"><!--" +
                "\n// some code here\nalert('Hello world');\n--></script><span>More text</span></div>" +
                "</body></html>";
        String expected = "<html><body><p>Some text </p>\n<p>\n\n</p><div>" +
                "<span>More text</span></div></body></html>";
        assertEquals(expected, new StripScriptModel(new Model<String>(multiLine)).getObject());
    }
}
