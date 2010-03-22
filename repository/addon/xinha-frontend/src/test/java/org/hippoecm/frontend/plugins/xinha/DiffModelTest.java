package org.hippoecm.frontend.plugins.xinha;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.junit.Before;
import org.junit.Test;
import org.outerj.daisy.diff.helper.NekoHtmlParser;
import org.outerj.daisy.diff.helper.SaxBuffer;
import org.outerj.daisy.diff.helper.SaxBuffer.SaxBit;
import org.outerj.daisy.diff.helper.SaxBuffer.StartElement;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DiffModelTest {

    @Before
    public void setUp() {
        HippoTester tester = new HippoTester();
        tester.startPage(Home.class);
    }

    static int countImages(String content) throws IOException, SAXException {
        int count = 0;
        NekoHtmlParser parser = new NekoHtmlParser();
        InputSource source = new InputSource();
        source.setCharacterStream(new StringReader(content));
        SaxBuffer buffer = parser.parse(source);
        for (SaxBit bit : buffer.getBits()) {
            if (bit instanceof StartElement) {
                if ("img".equals(((StartElement) bit).localName)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    public void removedImageIsShown() throws Exception {
        IModel<String> oldModel = new Model<String>("<html><body><img src=\"a\">abc</img></body></html>");
        IModel<String> newModel = new Model<String>("<html><body></body></html>");
        DiffModel dm = new DiffModel(oldModel, newModel);
        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }

    @Test
    public void addedImageIsShownOnce() throws Exception {
        IModel<String> oldModel = new Model<String>("<html><body></body></html>");
        IModel<String> newModel = new Model<String>("<html><body><img src=\"a\" /></body></html>");
        DiffModel dm = new DiffModel(oldModel, newModel);

        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }

    @Test
    public void constantImageIsShownOnce() throws Exception {
        IModel<String> oldModel = new Model<String>("<html><body><img src=\"a\" /></body></html>");
        IModel<String> newModel = new Model<String>("<html><body><img src=\"a\" /></body></html>");
        DiffModel dm = new DiffModel(oldModel, newModel);

        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }
}
