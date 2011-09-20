package org.onehippo.cms7.channelmanager.templatecomposer.iframe;

import java.util.List;
import java.util.NoSuchElementException;

import com.gargoylesoftware.htmlunit.html.HtmlElement;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CmsEditTest extends AbstractChannelManagerTest {

    @Before
    public void startPage() throws Exception {
        setUp("surfandedit.html");
        initializeIFrameHead();
        initializeTemplateComposer(false, true);
    }

    @Test
    public void testSurfAndEdit() throws Exception {
        assertTrue(!isMessageSend("iframeexception"));

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
        assertEquals(3, messages.size());
        assertEquals("edit-document", messages.get(2).messageTag);
        assertTrue(!isMessageSend("iframeexception"));
    }

}
