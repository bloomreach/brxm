/*
 *  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParseTextsTest  {

    private static final String HREF1 = "http://www.onehippo.org:80";
    private static final String HREF2 = "http://www.onehippo.org:80/2";
    private static final String HREF3 = "http://www.onehippo.org:80/3";
    private static final String MAILTO = "mailto://www.onehippo.org:80";
    private static final String SRC = "http://www.onehippo.org:80/img";

    @Test
    public void testSimpleHrefWithSingleOrDoubleAndWithoutQuotes() {
        String text = "hello <a href='http://www.onehippo.org:80'>hello</a> and <A HREF=http://www.example.com/2 >hello2</A> and  <A HREF=\"http://www.example.com/3 \" >hello3</A>";
        final List<String> links = PlainTextLinksExtractor.getLinks(text);
        assertEquals(3, links.size());
    }

    @Test
    public void testSimpleSrcWithSingleOrDoubleAndWithoutQuotes() {
        String text = "hello <img src='http://www.example.com'/> and <IMG Src=http://www.example.com/2 /> and  <IMG src=\"http://www.example.com/3 \" />";
        final List<String> links = PlainTextLinksExtractor.getLinks(text);
        assertEquals(3, links.size());
    }

    @Test
    public void testBrokenHTML() {
        String text = "hello <a href='http://www.onehippo.org:80/1'>first<a/> and <a href='http://www.onehippo.org:80/2'";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <a href='http://www.onehippo.org:80/1'>first<a/> and <a href='http://www.onehippo.org:80/2\"";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <a href='HTTP://www.onehippo.org:80/1'>first<a/> and <a href='http://www.onehippo.org:80/2";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <a href='http://www.onehippo.org:80/1'>first<a/> and <a href='";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <a href='HTTPS://www.onehippo.org:80/1'>first<a/> and <a href=";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <a href='http://www.onehippo.org:80/1'>first<a/> and <a href";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <img src='http://www.onehippo.org:80/1'/> and <img src='http://www.onehippo.org:80/2'";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <img src='http://www.onehippo.org:80/1'/> and <img src='http://www.onehippo.org:80/2\"";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <img src='http://www.onehippo.org:80/1'/> and <img src='http://www.onehippo.org:80/2";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <img src='http://www.onehippo.org:80/1'/> and <img src='";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <img src='http://www.onehippo.org:80/1'/> and <img src=";
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());

    }

    @Test
    public void testVeryBrokenHTML() {
        String text = "hello <img <a src='http://www.onehippo.org:80/1' /> \" /> and <img <a href=\"http://localhost:foo\" /> src='";
        // it finds a correct link src='http://www.onehippo.org:80/1' and "http://localhost:foo"
        assertEquals(2, PlainTextLinksExtractor.getLinks(text).size());

        text = "hello <img <a  />  \" src=http://www.onehippo.org:80/1 /> and <img <a href=\"http://localhost:foo\" /> src='";

        // it skips the first link src=http://www.onehippo.org:80/1 because there is an and tag from the <a element after the <img but before the src
        assertEquals(1, PlainTextLinksExtractor.getLinks(text).size());
    }

    @Test
    public void testTexts() {
        String text = "<html><body>\n<H1>Lorem ipsum dolor <B><A HREF="+HREF1+">Lorem Ipsum</A></B> <B><A HREF="+HREF1+">Lorem Ipsum</A></B> sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et " +
                "dolore magna aliqua. Ut enim ad minim veniam,  <B><A HREF="+HREF2+">Lorem Ipsum</A></B> quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat." +
                " Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat " +
                "cupidatat non proident,  <B><A HREF="+HREF3+">Lorem Ipsum</A></B> sunt in culpa" +
                " qui officia deserunt mollit anim id est laborum.</H1>" +
                "<P>Sed ut perspiciatis unde omnis <IMG SRC="+SRC+"/> iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa " +
                "quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit " +
                "aspernatur aut odit aut fugit, <A HREF="+MAILTO+">Lorem Ipsum</A> sed quia consequuntur <IMG SRC="+SRC+"/> magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est," +
                " qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore" +
                " magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut" +
                " aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil " +
                "molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?</P>\n</body></html>\n";

        final List<String> links = PlainTextLinksExtractor.getLinks(text);

        // 5 links expected:
        // Two times HREF1 but duplicates are collapsed in 1
        // One time HREF2 and one time HREF3
        // Two SRC but with same value
        // the mailto: does also count because the parser is not responsible for filtering out non http(s) urls by itself.
        assertEquals(5, links.size());

    }
}
