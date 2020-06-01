/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.PluginPage;
import org.htmlcleaner.HtmlCleaner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    
    static int countImages(String content) {
        return new HtmlCleaner().clean(content).getElementsByName("img", true).length;
    }

    @Test
    public void removedImageIsShown() {
        IModel<String> oldModel = new Model<>("<html><body><img src=\"a\">abc</img></body></html>");
        IModel<String> newModel = new Model<>("<html><body></body></html>");
        HtmlDiffModel dm = new HtmlDiffModel(oldModel, newModel);
        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }

    @Test
    public void addedImageIsShownOnce() {
        IModel<String> oldModel = new Model<>("<html><body></body></html>");
        IModel<String> newModel = new Model<>("<html><body><img src=\"a\" /></body></html>");
        HtmlDiffModel dm = new HtmlDiffModel(oldModel, newModel);

        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }

    @Test
    public void constantImageIsShownOnce() {
        IModel<String> oldModel = new Model<>("<html><body><img src=\"a\" /></body></html>");
        IModel<String> newModel = new Model<>("<html><body><img src=\"a\" /></body></html>");
        HtmlDiffModel dm = new HtmlDiffModel(oldModel, newModel);

        String content = dm.getObject();
        assertEquals(1, countImages(content));
    }
    
}
