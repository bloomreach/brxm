/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class TestHeadElementUtils {

    public Document buildDocumentFrom(final String xmlString) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        return docBuilder.parse(new InputSource(new StringReader(xmlString)));
    }

    @Test
    public void testTitleContribution() throws Exception {
        Document doc = buildDocumentFrom("<title>Hello World! Homepage</title>");

        Element headElem = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, false, false, false);
        assertEquals("<title>Hello World! Homepage</title>", sw.toString());
    }

    @Test
    public void testEmptyScriptContribution() throws Exception {
        Document doc = buildDocumentFrom("<script language=\"javascript\"></script>");
        Element headElem = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, false, false);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());

        headElem.setTextContent("");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, false, false);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());
    }

    @Test
    public void testScriptContribution() throws Exception {
        Document doc = buildDocumentFrom("<script language=\"javascript\">alert('Hello, World!');</script>");
        Element headElem = doc.getDocumentElement();
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, false, false);
        assertEquals("<script language=\"javascript\">alert('Hello, World!');</script>", sw.toString());
    }


    @Test
    public void testHtmlTagInScriptContribution() throws Exception {
        String xmlString = "<script type=\"text/javascript\">var jsflag = $('<html><body><span><input name=\"js_enabled\"></input></span></body></html>');</script>";
        Document doc = buildDocumentFrom(xmlString);

        Element element = doc.getDocumentElement();
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, false, false);
        assertEquals(xmlString, sw.toString());
    }

    @Test
    public void testHtmlTagParagraphContribution() throws Exception {
        String xmlString = "<p>This is a paragraph with a <strong>bold</strong> text</p>";
        Document doc = buildDocumentFrom(xmlString);

        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, false, false);
        assertEquals(xmlString, sw.toString());
    }

    @Test
    public void testEmptyOrBlankScriptContributionInCDATASection() throws Exception {
        Document doc = buildDocumentFrom("<script language=\"javascript\"></script>");
        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, false);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());

        element.setTextContent("");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, false);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());

        element.setTextContent(" ");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, false);
        assertEquals("<script language=\"javascript\"><![CDATA[ ]]></script>", sw.toString());
    }

    @Test
    public void testScriptContributionInCDATASection() throws Exception {
        Document doc = buildDocumentFrom("<script language=\"javascript\"><![CDATA[ alert('Hello, World!'); ]]></script>");
        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, false);

        assertEquals("<script language=\"javascript\"><![CDATA[ alert('Hello, World!'); ]]></script>", sw.toString());
    }
    
    @Test
    public void testEmptyOrBlankScriptContributionInCommentedOutCDATASection() throws Exception {
        Document doc = buildDocumentFrom("<script language=\"javascript\"></script>");
        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, true);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());
        
        element.setTextContent("");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, true);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());
        
        element.setTextContent(" ");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, true);
        assertEquals("<script language=\"javascript\">//<![CDATA[ //]]></script>", sw.toString());
    }
    
    @Test
    public void testScriptContributionInCommentedOutCDATASection() throws Exception {
        Document doc = buildDocumentFrom("<script language=\"javascript\">alert('Hello, World!');</script>");
        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, true);
        assertEquals("<script language=\"javascript\">//<![CDATA[alert('Hello, World!');//]]></script>", sw.toString());
    }
    
    @Test
    public void testStyleContribution() throws Exception {
        Document doc = buildDocumentFrom("<style>body { background-color: red }</style>");
        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, false, false);
        assertEquals("<style>body { background-color: red }</style>", sw.toString());
    }

    @Test
    public void testStyleContributionInCDATASection() throws Exception {
        Document doc = buildDocumentFrom("<style>body { background-color: red }</style>");
        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, false);
        assertEquals("<style><![CDATA[body { background-color: red }]]></style>", sw.toString());
    }
    
    @Test
    public void testStyleContributionInCommentedOutCDATASection() throws Exception {
        Document doc = buildDocumentFrom("<style>body { background-color: red }</style>");
        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, true);
        assertEquals("<style>/*<![CDATA[*/body { background-color: red }/*]]>*/</style>", sw.toString());
    }

    @Test
    public void testAttributesAccordingInsertionOrder() throws Exception {
        Document doc = buildDocumentFrom("<script language=\"javascript\" type=\"text/javascript\" src=\"test.js\"></script>");
        Element element = doc.getDocumentElement();

        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, element, true, true, true, false);
        assertEquals("<script language=\"javascript\" src=\"test.js\" type=\"text/javascript\"></script>", sw.toString());
    }

}
