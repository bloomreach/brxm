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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MarkupCodeUtils {
    private static final Logger log = LoggerFactory.getLogger(MarkupCodeUtils.class);
    private static final String TEMPLATE_INC_JSP = "<hst:include ref=\"$NAME$\"/>";
    private static final String TEMPLATE_INC_FTL = "<@hst.include ref=\"$NAME$\"/>";


    public static boolean hasHstInclude(final File target, final String name, final TemplateType type) {
        final String ftl = GlobalUtils.readTextFile(target.toPath()).toString();
        final Pattern pattern;
        if (type == TemplateType.FREEMARKER) {
            pattern = Pattern.compile("<@hst\\.include\\s+ref\\s*=\\s*\"" + name + "\"\\s*/>");
        } else {
            pattern = Pattern.compile("<hst:include\\s+ref\\s*=\\s*\"" + name + "\"\\s*/>");
        }
        final Matcher matcher = pattern.matcher(ftl);
        return matcher.find();
    }

    /**
     * Adds include tag as <strong>last</strong> child of {@code body} tag
     */
    public static boolean addHstIncludeAsLastBody(final File target, final String name, final TemplateType type) {
        return addHstInclude(target, name, type, "</body>", false);
    }

    /**
     * Adds include tag as <strong>first</strong> child of {@code body} tag
     */
    public static boolean addHstIncludeAsFirstBody(final File target, final String name, final TemplateType type) {
        return addHstInclude(target, name, type, "<body>", true);
    }


    public static boolean addHstInclude(final File target, final String name, final TemplateType type, final String targetTagName, final boolean addOffset) {
        if (hasHstInclude(target, name, type)) {
            log.info("HST include already exists: {}", name);
            return true;
        }
        final String template = GlobalUtils.readTextFile(target.toPath()).toString();
        final int idx = template.indexOf(targetTagName);
        if (idx == -1) {
            return false;
        }
        int offset = 0;
        if (addOffset) {
            offset = targetTagName.length();
        }
        final String first = template.substring(0, idx+offset);
        final String last = template.substring(idx+offset, template.length());
        final String include;
        if (type == TemplateType.JSP) {
            include = TEMPLATE_INC_JSP.replace("$NAME$", name);
        } else {
            include = TEMPLATE_INC_FTL.replace("$NAME$", name);
        }
        GlobalUtils.writeToFile(first + include + last, target.toPath());
        return true;
    }


    public enum TemplateType {
        JSP, FREEMARKER
    }

    private MarkupCodeUtils() {
    }
}
