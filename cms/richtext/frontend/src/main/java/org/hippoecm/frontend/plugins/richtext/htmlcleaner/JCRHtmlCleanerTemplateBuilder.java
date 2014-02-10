/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.htmlcleaner;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

import nl.hippo.htmlcleaner.HtmlCleanerTemplate;

/**
 * @deprecated
 * @see DeprecatedHtmlCleanerPlugin
 */
@Deprecated
public class JCRHtmlCleanerTemplateBuilder {

    public static final String HTMLCLEANER_PREFIX = "hippohtmlcleaner:";
    public static final String CLEANUP = HTMLCLEANER_PREFIX + "cleanup";
    public static final String SCHEMA = HTMLCLEANER_PREFIX + "schema";
    public static final String ALLOWED_DIV_CLASSES = HTMLCLEANER_PREFIX + "allowedDivClasses";
    public static final String ALLOWED_SPAN_CLASSES = HTMLCLEANER_PREFIX + "allowedSpanClasses";
    public static final String ALLOWED_PRE_CLASSES = HTMLCLEANER_PREFIX + "allowedPreClasses";
    public static final String ALLOWED_PARA_CLASSES = HTMLCLEANER_PREFIX + "allowedParaClasses";
    public static final String IMG_ALTERNATE_SRC_ATTR = HTMLCLEANER_PREFIX + "imgAlternateSrcAttr";
    public static final String CLEANUP_ELEMENT = HTMLCLEANER_PREFIX + "cleanupElement";
    public static final String ALLOWED_SINGLE_WHITESPACE_ELEMENT = HTMLCLEANER_PREFIX
            + "allowedSingleWhitespaceElement";
    public static final String FORCE_EMPTY_FIELD = HTMLCLEANER_PREFIX + "forceEmptyField";

    public static final String NAME = HTMLCLEANER_PREFIX + "name";
    public static final String CLASSES = HTMLCLEANER_PREFIX + "classes";
    public static final String ATTRIBUTES = HTMLCLEANER_PREFIX + "attributes";
    public static final String SERIALIZATION = HTMLCLEANER_PREFIX + "serialization";
    public static final String LINEWIDTH = HTMLCLEANER_PREFIX + "linewidth";
    public static final String SERIALIZATION_ELEMENT = HTMLCLEANER_PREFIX + "serializationElement";
    public static final String NEW_LINES_AFTER_OPEN = HTMLCLEANER_PREFIX + "newLinesAfterOpen";
    public static final String NEW_LINES_AFTER_CLOSE = HTMLCLEANER_PREFIX + "newLinesAfterClose";
    public static final String NEW_LINES_BEFORE_OPEN = HTMLCLEANER_PREFIX + "newLinesBeforeOpen";
    public static final String NEW_LINES_BEFORE_CLOSE = HTMLCLEANER_PREFIX + "newLinesBeforeClose";
    public static final String INLINE = HTMLCLEANER_PREFIX + "inline";

    public static final String SCHEMA_TRANSITIONAL = "transitional";
    public static final String SCHEMA_STRICT = "strict";
    public static final String DEFAULT_SCHEMA = SCHEMA_TRANSITIONAL;

    public static final int DEFAULT_NEW_LINES = 0;
    public static final boolean DEFAULT_INLINE = false;

    public HtmlCleanerTemplate buildTemplate(IPluginConfig cleanerConfig) throws Exception {
        HtmlCleanerTemplate template = null;
        if (cleanerConfig != null) {
            template = new HtmlCleanerTemplate();

            handleCleanup(template, getRequiredNode(cleanerConfig, CLEANUP));
            handleSerialization(template, getRequiredNode(cleanerConfig, SERIALIZATION));

            template.initialize();
        }
        return template;
    }

    /**
     *  XHTML Schema element: e.g. 'transitional' or 'strict'
     * @param template
     * @param schema
     */
    protected void handleXhtmlSchema(HtmlCleanerTemplate template, String schema) {
        if (StringUtils.isEmpty(schema)) {
            template.setXhtmlSchema(DEFAULT_SCHEMA);
        } else {
            template.setXhtmlSchema(schema);
        }
    }

    private IPluginConfig getRequiredNode(IPluginConfig cfg, String nodeName) throws Exception {
        IPluginConfig requiredCfg = cfg.getPluginConfig(nodeName);
        if (requiredCfg == null) {
            throw new Exception("HtmlCleaner configuration: Missing required node " + nodeName);
        }
        return requiredCfg;
    }

    private Object getRequiredValue(String parentElementDescription, Map m, String propName) throws Exception {
        Object o = m.get(propName);
        if (o == null) {
            throw new Exception("HtmlCleaner configuration: Missing required value " + propName + " for "
                    + parentElementDescription);
        }
        return o;
    }

    private String getRequiredStringValue(String parentElementDescription, Map m, String propName) throws Exception {
        String v = (String) getRequiredValue(parentElementDescription, m, propName);
        if (StringUtils.isEmpty(v)) {
            throw new Exception("HtmlCleaner configuration: Required string value '" + propName + "' is empty for "
                    + parentElementDescription);
        }
        return v;
    }

    protected void handleAllowedDivClasses(HtmlCleanerTemplate template, String[] cssClasses) {
        if (cssClasses != null) {
            for (String cssClass : cssClasses) {
                template.addAllowedDivClass(cssClass);
            }
        }
    }

    protected void handleAllowedSpanClasses(HtmlCleanerTemplate template, String[] cssClasses) {
        if (cssClasses != null) {
            for (String cssClass : cssClasses) {
                template.addAllowedSpanClass(cssClass);
            }
        }
    }

    protected void handleAllowedPreClasses(HtmlCleanerTemplate template, String[] cssClasses) {
        if (cssClasses != null) {
            for (String cssClass : cssClasses) {
                template.addAllowedPreClass(cssClass);
            }
        }
    }

    protected void handleAllowedParaClasses(HtmlCleanerTemplate template, String[] cssClasses) {
        if (cssClasses != null) {
            for (String cssClass : cssClasses) {
                template.addAllowedParaClass(cssClass);
            }
        }
    }

    protected void handleAllowedElements(HtmlCleanerTemplate template, List<? extends Map> elements) throws Exception {
        if (elements != null) {
            for (Map elMap : elements) {
                String name = getRequiredStringValue("allowed element (" + CLEANUP_ELEMENT + ")", elMap, NAME);
                String[] attributes = (String[]) elMap.get(ATTRIBUTES);
                template.addAllowedElement(name, attributes != null ? attributes : new String[] {});
            }
        }
    }

    protected void handleAllowedSingleWhitespaceElements(HtmlCleanerTemplate template, List<? extends Map> elements)
            throws Exception {
        if (elements != null) {
            for (Map elMap : elements) {
                String name = getRequiredStringValue("allowed element (" + ALLOWED_SINGLE_WHITESPACE_ELEMENT + ")",
                        elMap, NAME);
                boolean forceNonBreakingSpace = false;
                String key = HTMLCLEANER_PREFIX + "forceNonBreakingSpace";
                if (elMap.containsKey(key)) {
                    forceNonBreakingSpace = (Boolean) elMap.get(key);
                }
                boolean ignoreTrailingNewLineCharacter = true;
                key = HTMLCLEANER_PREFIX + "ignoreTrailingNewLineCharacter";
                if (elMap.containsKey(key)) {
                    ignoreTrailingNewLineCharacter = (Boolean) elMap.get(key);
                }

                template.addAllowedSingleWhitespaceElement(name, forceNonBreakingSpace, ignoreTrailingNewLineCharacter);
            }
        }
    }

    protected void handleImgAlternateSrcAttr(HtmlCleanerTemplate template, String value) {
        if (!StringUtils.isEmpty(value)) {
            template.setXhtmlSchema(value);
        }
    }

    protected void handleCleanup(HtmlCleanerTemplate template, IPluginConfig c) throws Exception {
        // Process Xhtml schema
        handleXhtmlSchema(template, (String) c.get(SCHEMA));
        // Process Allowed SPAN css classes
        handleAllowedSpanClasses(template, (String[]) c.get(ALLOWED_SPAN_CLASSES));
        // Process Allowed DIV css classes
        handleAllowedDivClasses(template, (String[]) c.get(ALLOWED_DIV_CLASSES));
        // Process Allowed PARA css classes
        handleAllowedParaClasses(template, (String[]) c.get(ALLOWED_PARA_CLASSES));
        // Process Allowed PRE css classes
        handleAllowedPreClasses(template, (String[]) c.get(ALLOWED_PRE_CLASSES));
        // Process alternate source attribute voor IMG elements
        handleImgAlternateSrcAttr(template, (String) c.get(IMG_ALTERNATE_SRC_ATTR));
        // Process allowed HTML elements
        handleAllowedElements(template, (List<? extends Map>) c.get(CLEANUP_ELEMENT));
        // Process allowed elements with a single whitespace character
        handleAllowedSingleWhitespaceElements(template, (List<? extends Map>) c.get(ALLOWED_SINGLE_WHITESPACE_ELEMENT));

        template.setForceEmptyField(c.getBoolean(FORCE_EMPTY_FIELD));
    }

    protected void handleLineWidth(HtmlCleanerTemplate template, int lineWidth) {
        template.setMaxLineWidth(lineWidth);
    }

    protected void handleSerializationElements(HtmlCleanerTemplate template, List<? extends Map> elements)
            throws Exception {
        if (elements != null) {
            for (Map m : elements) {
                String name = getRequiredStringValue("allowed element (" + CLEANUP_ELEMENT + ")", m, NAME);
                Long newLinesAfterOpen = (Long) m.get(NEW_LINES_AFTER_OPEN);
                Long newLinesAfterClose = (Long) m.get(NEW_LINES_AFTER_CLOSE);
                Long newLinesBeforeOpen = (Long) m.get(NEW_LINES_BEFORE_OPEN);
                Long newLinesBeforeClose = (Long) m.get(NEW_LINES_BEFORE_CLOSE);
                Boolean inline = (Boolean) m.get(INLINE);
                template.addOutputElement(name, newLinesBeforeOpen != null ? newLinesBeforeOpen.intValue()
                        : DEFAULT_NEW_LINES, newLinesAfterOpen != null ? newLinesAfterOpen.intValue()
                        : DEFAULT_NEW_LINES, newLinesBeforeClose != null ? newLinesBeforeClose.intValue()
                        : DEFAULT_NEW_LINES, newLinesAfterClose != null ? newLinesAfterClose.intValue()
                        : DEFAULT_NEW_LINES, inline != null ? inline.booleanValue() : DEFAULT_INLINE);
            }
        }
    }

    protected void checkRequiredPropertyPresent(Map m, String name) throws Exception {
        if (m.get(name) == null) {
            throw new Exception("Required property missing: " + name);
        }
    }

    protected void handleSerialization(HtmlCleanerTemplate template, IPluginConfig c) throws Exception {
        // Process line width
        checkRequiredPropertyPresent(c, LINEWIDTH);

        handleLineWidth(template, c.getInt(LINEWIDTH));
        // Process serialization elements 
        handleSerializationElements(template, (List<? extends Map>) c.get(SERIALIZATION_ELEMENT));
    }

}
