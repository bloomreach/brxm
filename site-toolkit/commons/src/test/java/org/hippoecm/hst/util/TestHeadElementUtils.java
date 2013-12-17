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

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.hippoecm.hst.core.component.HeadElement;
import org.hippoecm.hst.core.component.HeadElementImpl;
import org.junit.Test;

public class TestHeadElementUtils {

    @Test
    public void testTitleContribution() throws Exception {
        HeadElement headElem = new HeadElementImpl("title");
        headElem.setTextContent("Hello World! Homepage");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, false, false, false);
        assertEquals("<title>Hello World! Homepage</title>", sw.toString());
    }
    
    @Test
    public void testEmptyScriptContribution() throws Exception {
        HeadElement headElem = new HeadElementImpl("script");
        headElem.setAttribute("language", "javascript");
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
        HeadElement headElem = new HeadElementImpl("script");
        headElem.setAttribute("language", "javascript");
        headElem.setTextContent("alert('Hello, World!');");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, false, false);
        assertEquals("<script language=\"javascript\">alert('Hello, World!');</script>", sw.toString());
    }
    
    @Test
    public void testEmptyOrBlankScriptContributionInCDATASection() throws Exception {
        HeadElement headElem = new HeadElementImpl("script");
        headElem.setAttribute("language", "javascript");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, false);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());
        
        headElem.setTextContent("");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, false);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());
        
        headElem.setTextContent(" ");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, false);
        assertEquals("<script language=\"javascript\"><![CDATA[ ]]></script>", sw.toString());
    }

    @Test
    public void testScriptContributionInCDATASection() throws Exception {
        HeadElement headElem = new HeadElementImpl("script");
        headElem.setAttribute("language", "javascript");
        headElem.setTextContent("alert('Hello, World!');");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, false);
        assertEquals("<script language=\"javascript\"><![CDATA[alert('Hello, World!');]]></script>", sw.toString());
    }
    
    @Test
    public void testEmptyOrBlankScriptContributionInCommentedOutCDATASection() throws Exception {
        HeadElement headElem = new HeadElementImpl("script");
        headElem.setAttribute("language", "javascript");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, true);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());
        
        headElem.setTextContent("");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, true);
        assertEquals("<script language=\"javascript\"></script>", sw.toString());
        
        headElem.setTextContent(" ");
        sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, true);
        assertEquals("<script language=\"javascript\">\n//<![CDATA[\n \n//]]>\n</script>", sw.toString());
    }
    
    @Test
    public void testScriptContributionInCommentedOutCDATASection() throws Exception {
        HeadElement headElem = new HeadElementImpl("script");
        headElem.setAttribute("language", "javascript");
        headElem.setTextContent("alert('Hello, World!');");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, true);
        assertEquals("<script language=\"javascript\">\n//<![CDATA[\nalert('Hello, World!');\n//]]>\n</script>", sw.toString());
    }
    
    @Test
    public void testStyleContribution() throws Exception {
        HeadElement headElem = new HeadElementImpl("style");
        headElem.setTextContent("body { background-color: red }");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, false, false);
        assertEquals("<style>body { background-color: red }</style>", sw.toString());
    }

    @Test
    public void testStyleContributionInCDATASection() throws Exception {
        HeadElement headElem = new HeadElementImpl("style");
        headElem.setTextContent("body { background-color: red }");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, false);
        assertEquals("<style><![CDATA[body { background-color: red }]]></style>", sw.toString());
    }
    
    @Test
    public void testStyleContributionInCommentedOutCDATASection() throws Exception {
        HeadElement headElem = new HeadElementImpl("style");
        headElem.setTextContent("body { background-color: red }");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, true);
        assertEquals("<style>\n/*<![CDATA[*/\nbody { background-color: red }\n/*]]>*/\n</style>", sw.toString());
    }


    @Test
    public void testAttributesAccordingInsertionOrder() throws Exception {
        HeadElement headElem = new HeadElementImpl("script");
        headElem.setAttribute("language", "javascript");
        headElem.setAttribute("type", "text/javascript");
        headElem.setAttribute("src", "test.js");
        StringWriter sw = new StringWriter();
        HeadElementUtils.writeHeadElement(sw, headElem, true, true, true, false);
        assertEquals("<script language=\"javascript\" type=\"text/javascript\" src=\"test.js\"></script>", sw.toString());
    }

}
