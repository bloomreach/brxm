/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.ckeditor;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default configuration of CKEditor fields in Hippo CMS.
 */
public class CKEditorConfig {

    private CKEditorConfig() {
    }

    private static final Logger log = LoggerFactory.getLogger(CKEditorConfig.class);
    
    /**
     * Various CKEDITOR.config property names.
     */
    public static final String CONTENTS_CSS = "contentsCss";
    public static final String CUSTOM_CONFIG = "customConfig";
    public static final String EXTRA_PLUGINS = "extraPlugins";
    public static final String KEYSTROKES = "keystrokes";
    public static final String LANGUAGE = "language";
    public static final String STYLES_SET = "stylesSet";

    /**
     * CKEDITOR constants for keyboard shortcuts
     */
    public static final int CTRL = 0x110000;
    public static final int SHIFT = 0x220000;
    public static final int ALT = 0x440000;

    /**
     * Default config for formatted text fields.
     */
    public static final String DEFAULT_FORMATTED_TEXT_CONFIG = "{"
            // do not html encode but utf-8 encode hence entities = false
            + "  entities: false,"
            // &gt; must not be replaced with > hence basicEntities = true
            + "  basicEntities: true,"
            + "  autoUpdateElement: false,"
            + "  contentsCss: ['ckeditor/hippocontents.css'],"
            + "  plugins: 'basicstyles,button,clipboard,contextmenu,copyformatting,divarea,enterkey,entities,floatingspace,floatpanel,htmlwriter,listblock,magicline,menu,menubutton,panel,panelbutton,removeformat,richcombo,stylescombo,tab,toolbar,undo',"
            + "  title: false,"
            + "  toolbar: ["
            + "    { name: 'styles', items: [ 'Styles' ] },"
            + "    { name: 'basicstyles', items: [ 'Bold', 'Italic', 'Underline', '-', 'CopyFormatting', 'RemoveFormat' ] },"
            + "    { name: 'clipboard', items: [ 'Undo', 'Redo' ] }"
            + "  ],"
            + "  hippo: { "
            + "    hasBottomToolbar: false"
            + "  }"
            + "}";

    /**
     * Default config for rich text fields.
     */
    public static final String DEFAULT_RICH_TEXT_CONFIG = "{"
            // do not html encode but utf-8 encode hence entities = false
            + "  entities: false,"
            // &gt; must not be replaced with > hence basicEntities = true
            + "  basicEntities: true,"
            + "  autoUpdateElement: false,"
            + "  contentsCss: ['ckeditor/hippocontents.css'],"
            + "  dialog_buttonsOrder: 'ltr',"
            + "  dialog_noConfirmCancel: true,"
            + "  extraAllowedContent: {"
            + "    embed: {"
            + "      attributes: 'allowscriptaccess,height,src,type,width'"
            + "    },"
            + "    img: {"
            + "      attributes: 'border,hspace,vspace'"
            + "    },"
            + "    object: {"
            + "      attributes: 'align,data,height,id,title,type,width'"
            + "    },"
            + "    p: {"
            + "      attributes: 'align'"
            + "    },"
            + "    param: {"
            + "      attributes: 'name,value'"
            + "    },"
            + "    table: {"
            + "      attributes: 'width'"
            + "    },"
            + "    td: {"
            + "      attributes: 'valign,width'"
            + "    },"
            + "    th: {"
            + "      attributes: 'valign,width'"
            + "    }"
            + "  },"
			+ "  keystrokes: ["
            + "    [ 'Ctrl', 'm', 'maximize' ],"
            + "    [ 'Alt', 'b', 'showblocks' ]"
            + "  ],"
            + "  linkShowAdvancedTab: false,"
            + "  plugins: 'a11yhelp,basicstyles,button,clipboard,codemirror,contextmenu,copyformatting,dialog,dialogadvtab,dialogui,divarea,elementspath,enterkey,entities,floatingspace,floatpanel,hippopicker,htmlwriter,indent,indentblock,indentlist,justify,link,list,listblock,liststyle,magicline,maximize,menu,menubutton,panel,panelbutton,pastefromword,pastetext,popup,removeformat,resize,richcombo,showblocks,showborders,specialchar,stylescombo,tab,table,tableresize,tableselection,tabletools,textselection,toolbar,undo,youtube',"
            + "  removeFormatAttributes: 'style,lang,width,height,align,hspace,valign',"
            + "  title: false,"
            + "  toolbarGroups: ["
            + "    { name: 'styles' },"
            + "    { name: 'basicstyles' },"
            + "    { name: 'cleanup' },"
            + "    { name: 'undo' },"
            + "    { name: 'listindentalign',  groups: [ 'list', 'indent', 'align' ] },"
            + "    { name: 'links' },"
            + "    { name: 'insert' },"
            + "    { name: 'tools' },"
            + "    { name: 'mode' }"
            + "  ],"
            + "  hippo: { "
            + "    hasBottomToolbar: false"
            + "  }"
            + "}";

    private static final String STYLES_SET_LANGUAGE_PARAM = "{language}";

    public static ObjectNode combineConfig(final String defaultJson, final String overlayedJson, final String appendedJson) throws IOException {
        final ObjectNode json = Json.object(defaultJson);
        logConfig("Default CKEditor config", json);

        Json.overlay(json, checkJson("overlayed", overlayedJson));
        logConfig("Overlayed CKEditor config", json);

        Json.append(json, checkJson("appended", appendedJson));
        logConfig("Final CKEditor config", json);

        return json;
    }

    private static JsonNode checkJson(final String name, final String json) {
        try {
            return Json.object(json);
        } catch (IOException e) {
            final String msg = "Ignoring CKEditor " + name + " configuration. Not valid JSON: '" + json + "'";
            if (log.isDebugEnabled()) {
                log.warn(msg, e);
            } else {
                log.warn(msg);
            }
        }
        return Json.object();
    }

    private static void logConfig(final String name, final ObjectNode config) {
        if (log.isDebugEnabled()) {
            log.debug(name + "\n" + Json.prettyString(config));
        }
    }

    public static ObjectNode setDefaults(final ObjectNode config, final String language) {
        config.put(CKEditorConfig.LANGUAGE, language);

        // convert Hippo-specific 'declarative' keystrokes to numeric ones
        final JsonNode declarativeAndNumericKeystrokes = config.get(CKEditorConfig.KEYSTROKES);
        final ArrayNode numericKeystrokes = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(declarativeAndNumericKeystrokes);
        config.set(CKEditorConfig.KEYSTROKES, numericKeystrokes);

        // load the localized hippo styles if no other styles are specified
        final String stylesSet = config.has(STYLES_SET) ? config.get(STYLES_SET).asText() : defaultStylesSet(language);
        final String localizedStylesSet = stylesSet.replace(STYLES_SET_LANGUAGE_PARAM, language);
        config.put(CKEditorConfig.STYLES_SET, localizedStylesSet);

        // disable custom config loading if not configured
        if (!config.has(CUSTOM_CONFIG)) {
            config.put(CUSTOM_CONFIG, StringUtils.EMPTY);
        }

        return config;
    }

    private static String defaultStylesSet(final String language) {
        final String styleName = "hippo_" + language;
        return styleName + ":./hippostyles.js";
    }
}
