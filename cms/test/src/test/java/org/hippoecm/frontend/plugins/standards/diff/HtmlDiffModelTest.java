/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.standards.diff;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.PluginPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.outerj.daisy.diff.helper.NekoHtmlParser;
import org.outerj.daisy.diff.helper.SaxBuffer;
import org.outerj.daisy.diff.helper.SaxBuffer.SaxBit;
import org.outerj.daisy.diff.helper.SaxBuffer.StartElement;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HtmlDiffModelTest {

    private HippoTester tester;

    @Before
    public void setUp() {
        tester = new HippoTester();
        tester.startPage(PluginPage.class);
    }

    @After
    public void tearDown() {
        tester.destroy();
        tester = null;
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
        HtmlDiffModel dm = new HtmlDiffModel(oldModel, newModel);
        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }

    @Test
    public void addedImageIsShownOnce() throws Exception {
        IModel<String> oldModel = new Model<String>("<html><body></body></html>");
        IModel<String> newModel = new Model<String>("<html><body><img src=\"a\" /></body></html>");
        HtmlDiffModel dm = new HtmlDiffModel(oldModel, newModel);

        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }

    @Test
    public void constantImageIsShownOnce() throws Exception {
        IModel<String> oldModel = new Model<String>("<html><body><img src=\"a\" /></body></html>");
        IModel<String> newModel = new Model<String>("<html><body><img src=\"a\" /></body></html>");
        HtmlDiffModel dm = new HtmlDiffModel(oldModel, newModel);

        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }
    
}
