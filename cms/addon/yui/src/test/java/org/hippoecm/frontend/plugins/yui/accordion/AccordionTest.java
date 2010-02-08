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
package org.hippoecm.frontend.plugins.yui.accordion;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.hippoecm.frontend.plugins.yui.YuiPage;
import org.hippoecm.frontend.plugins.yui.YuiTest;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlElement;

public class AccordionTest extends YuiTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static class Page extends YuiPage {

        public Page() {
            setOutputMarkupId(true);

            WebMarkupContainer container = new WebMarkupContainer("container");
            container.setOutputMarkupId(true);

            AccordionConfiguration config = new AccordionConfiguration();
            config.setRegisterRenderListener(false);
            config.setRegisterResizeListener(false);
            AccordionManagerBehavior amb = new AccordionManagerBehavior(getYuiManager(), config);

            container.add(amb);

            WebMarkupContainer first = new WebMarkupContainer("first");
            first.setOutputMarkupId(true);
            first.add(amb.newSection());
            container.add(first);

            WebMarkupContainer second = new WebMarkupContainer("second");
            second.setOutputMarkupId(true);
            second.add(amb.newSection());
            container.add(second);

            add(container);
        }
    }

    @Test
    public void testAccordion() throws Exception {
        setUp(Page.class);

        List<HtmlElement> elements = page.getElementById("first2").getElementsByAttribute("div", "class",
                "hippo-accordion-unit-center");
        String style = elements.get(0).getAttribute("style");
        assertEquals("height: 30px;", style);
    }

}
