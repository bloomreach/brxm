package org.onehippo.cms7.channelmanager.templatecomposer.iframe;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

import org.junit.Before;
import org.junit.Test;

import net.sourceforge.htmlunit.corejs.javascript.BaseFunction;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
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

        final List<String> messages = new LinkedList<String>();
        Window window = (Window) page.getWebClient().getCurrentWindow().getScriptObject();
        ScriptableObject.putProperty(window, "sendMessage", new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                messages.add((String) args[1]);
                return null;
            }

        });

        HtmlElement link = getLink();
        link.click();

        assertEquals(1, messages.size());
        assertEquals("edit-document", messages.get(0));
    }
}
