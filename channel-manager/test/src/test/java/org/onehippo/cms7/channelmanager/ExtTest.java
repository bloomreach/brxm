package org.onehippo.cms7.channelmanager;

import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlElement;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExtTest extends AbstractJavascriptTest {

    @Test
    public void runBasicExtTest() throws Exception {
        setUp("extjs-test.html");

        // TODO: replace by retry loop
        Thread.sleep(1000);

//        System.out.print(page.asXml());

        final HtmlElement result = page.getElementById("result");
        assertNotNull(result);
        assertTrue(result.getTextContent().contains("pass"));
    }

    // TODO fix test
//    @Test
//    public void runPageEditorTest() throws Exception {
//        setUp("pageeditor-test.html");
//
//        // TODO: replace by retry loop
//        Thread.sleep(1000);
//
//        System.out.print(page.asXml());
//
//        final HtmlElement result = page.getElementById("result");
//        assertNotNull(result);
//
//        assertTrue(result.getTextContent().contains("pass"));
//
//        final HtmlElement instance = page.getElementById("Hippo.ChannelManager.TemplateComposer.Instance");
//        assertNotNull(instance);
//        List<?> buttons = instance.getByXPath("//button");
//        assertEquals(1, buttons.size());
//    }
}
