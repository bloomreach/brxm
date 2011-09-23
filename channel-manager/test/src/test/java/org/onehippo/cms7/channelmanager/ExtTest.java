package org.onehippo.cms7.channelmanager;

import com.gargoylesoftware.htmlunit.html.HtmlElement;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExtTest extends AbstractJavascriptTest {

    @Test
    public void runExtTests() throws Exception {
        setUp("extjs-test.html");

        final HtmlElement result = page.getElementById("result");
        assertNotNull(result);
        assertTrue(result.getTextContent().contains("pass"));
    }

}
