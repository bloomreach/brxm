package org.onehippo.cms7.channelmanager.templatecomposer.iframe;

import java.util.List;
import java.util.NoSuchElementException;

import com.gargoylesoftware.htmlunit.html.HtmlElement;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CmsEditTest extends AbstractTemplateComposerTest {

    @Before
    public void startPage() throws Exception {
        setUp("cmseditlink.html");
        initializeIFrameHead();
        initializeTemplateComposer(false, true);
    }

    @Test
    public void testSurfAndEdit() throws Exception {
        assertFalse(isMessageSend("iframeexception"));

        // test if container is present
        HtmlElement link = getLink();
        assertTrue(isMetaDataConsumed(link));
    }

    private HtmlElement getLink() {
        final List<HtmlElement> divs = page.getElementsByTagName("a");
        for (HtmlElement div : divs) {
            if (eval("HST.CLASS.EDITLINK").equals(div.getAttribute("class"))) {
                return div;
            }
        }
        throw new NoSuchElementException();
    }

    @Test
    public void clickLinkSendsMessage() throws Exception {
        HtmlElement link = getLink();
        link.click();

        final List<Message> messages = getMessagesSend();
        assertEquals("init", messages.get(0).messageTag);
        assertEquals("edit-document", messages.get(1).messageTag);
        assertFalse(isMessageSend("iframeexception"));
    }

}
