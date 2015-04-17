/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class MarkupCodeUtilsTest {
    private static final Logger log = LoggerFactory.getLogger(MarkupCodeUtilsTest.class);
    public static final String FTL_TEMPLATE = "<!doctype html>\n" +
            "<#include \"../include/imports.ftl\">\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\"/>\n" +
            "    <link rel=\"stylesheet\" href=\"<@hst.webfile  path=\"/css/bootstrap.css\"/>\" type=\"text/css\"/>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"container\">\n" +
            "    <div class=\"row\">\n" +
            "        <div class=\"col-md-6 col-md-offset-3\">\n" +
            "        <@hst.include ref=\"top\"/>\n" +
            "        <@hst.include ref=\"menu\"/>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<@hst.headContributions categoryIncludes=\"htmlBodyEnd\" xhtml=true/>\n" +
            "</body>\n" +
            "</html>";
    public static final String JSP_TEMPLATE = "<!doctype html>\n" +
            "<%@ include file=\"/WEB-INF/jsp/include/imports.jsp\" %>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\"/>\n" +
            "  <link rel=\"stylesheet\" href=\"<hst:webfile  path=\"/css/bootstrap.css\"/>\" type=\"text/css\"/>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"container\">\n" +
            "  <div class=\"row\">\n" +
            "    <div class=\"col-md-6 col-md-offset-3\">\n" +
            "      <hst:include ref=\"top\"/>\n" +
            "      <hst:include ref=\"menu\"/>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "</div>\n" +
            "<hst:headContributions categoryIncludes=\"htmlBodyEnd\" xhtml=\"true\"/>\n" +
            "</body>\n" +
            "</html>";
    private File jspTempFile;
    private File ftlTempFile;

    @Before
    public void setUp() throws Exception {
        jspTempFile = File.createTempFile("testReplaceInclude", "jsp");
        ftlTempFile = File.createTempFile("testReplaceInclude", "ftl");
    }

    @Test
    public void testReplaceIncludeJsp() throws Exception {
        GlobalUtils.writeToFile(JSP_TEMPLATE, jspTempFile.toPath());
        final boolean top = MarkupCodeUtils.hasHstInclude(jspTempFile, "top", MarkupCodeUtils.TemplateType.JSP);
        assertTrue("Expected to find hst include 'top'", top);
        final boolean menu = MarkupCodeUtils.hasHstInclude(jspTempFile, "menu", MarkupCodeUtils.TemplateType.JSP);
        assertTrue("Expected to find hst include 'menu'", menu);
        MarkupCodeUtils.addHstIncludeAsFirstBody(jspTempFile, "test", MarkupCodeUtils.TemplateType.JSP);
        printTemplate();
        final boolean test = MarkupCodeUtils.hasHstInclude(jspTempFile, "test", MarkupCodeUtils.TemplateType.JSP);
        assertTrue("Expected to find hst include 'test'", test);

    }

    @Test
    public void testReplaceIncludeFtl() throws Exception {

        GlobalUtils.writeToFile(FTL_TEMPLATE, ftlTempFile.toPath());
        final boolean top = MarkupCodeUtils.hasHstInclude(ftlTempFile, "top", MarkupCodeUtils.TemplateType.FREEMARKER);
        assertTrue("Expected to find hst include 'top'", top);
        final boolean menu = MarkupCodeUtils.hasHstInclude(ftlTempFile, "menu", MarkupCodeUtils.TemplateType.FREEMARKER);
        assertTrue("Expected to find hst include 'menu'", menu);
        MarkupCodeUtils.addHstIncludeAsFirstBody(ftlTempFile, "test", MarkupCodeUtils.TemplateType.FREEMARKER);
        printTemplate();
        boolean test = MarkupCodeUtils.hasHstInclude(ftlTempFile, "test", MarkupCodeUtils.TemplateType.FREEMARKER);
        assertTrue("Expected to find hst include 'test'", test);
        GlobalUtils.writeToFile(FTL_TEMPLATE, ftlTempFile.toPath());
        MarkupCodeUtils.addHstIncludeAsLastBody(ftlTempFile, "test", MarkupCodeUtils.TemplateType.FREEMARKER);
        printTemplate();
        test = MarkupCodeUtils.hasHstInclude(ftlTempFile, "test", MarkupCodeUtils.TemplateType.FREEMARKER);
        assertTrue("Expected to find hst include 'test'", test);


    }

    private void printTemplate() {
        final StringBuilder builder = GlobalUtils.readTextFile(ftlTempFile.toPath());
        log.info("builder {}", builder);
    }

    @After
    public void tearDown() throws Exception {
        if (jspTempFile != null) {
            jspTempFile.delete();
        }
        if (ftlTempFile != null) {
            ftlTempFile.delete();
        }

    }
}