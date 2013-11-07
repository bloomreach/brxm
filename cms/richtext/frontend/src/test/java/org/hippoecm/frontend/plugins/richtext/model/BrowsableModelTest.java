package org.hippoecm.frontend.plugins.richtext.model;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.richtext.ILinkDecorator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link BrowsableModel}.
 */
public class BrowsableModelTest {

    @Test(expected = UnsupportedOperationException.class)
    public void setObjectIsNotSupported() {
        final BrowsableModel model = createModel("");
        model.setObject("");
    }

    @Test
    public void nullTextIsNotChanged() {
        assertGet(null, null);
    }

    @Test
    public void emptyTextIsNotChanged() {
        assertGet("", "");
    }

    @Test
    public void internalLinkIsRewritten() {
        assertGet("<a href=\"internal:name-of-facet\">internal link</a>", "<a href=\"name-of-facet\">internal link</a>");
    }

    @Test
    public void externalLinkIsRewritten() {
        assertGet("<a href=\"external:http://www.onehippo.org\">external link</a>", "<a href=\"http://www.onehippo.org\">external link</a>");
    }

    private static void assertGet(final String expectedText, final String storedText) {
        final BrowsableModel model = createModel(storedText);
        assertEquals(expectedText, model.getObject());
    }

    private static BrowsableModel createModel(final String text) {
        IModel<String> textModel = new Model(text);
        return new BrowsableModel(textModel, new PrefixingLinkDecorator());
    }

    private static class PrefixingLinkDecorator implements ILinkDecorator {

        @Override
        public String internalLink(final String link) {
            return "href=\"internal:" + link + "\"";
        }

        @Override
        public String externalLink(final String link) {
            return "href=\"external:" + link + "\"";
        }
    }

}
