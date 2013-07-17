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
package org.onehippo.cms7.reports.plugins;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ITranslateService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;

/**
 * Base class of all reports on the reporting dashboard.
 */
@ExtClass("Hippo.Reports.Portlet")
public class ReportPanel extends ExtPanel implements IStringResourceProvider {

    private static final long serialVersionUID = 1L;

    private static final PackageResourceReference REPORT_PANEL_BG_IMAGE = new PackageResourceReference(ReportPanel.class, "report-panel-bg.gif");
    public static final JavaScriptResourceReference REPORTS_PORTLET_JS = new JavaScriptResourceReference(ReportPanel.class, "Hippo.Reports.Portlet.js");

    private enum TitleColor { normal, highlighted, alert }
    private enum TitleSize { normal, large }

    private static final String CONFIG_BACKGROUND = "background";
    private static final String CONFIG_HEIGHT = "height";
    private static final String CONFIG_TITLE_COLOR = "title.color";
    private static final String CONFIG_TITLE_SIZE = "title.size";
    private static final String CONFIG_WIDTH = "width";

    private static final boolean DEFAULT_BACKGROUND = false;
    private static final int DEFAULT_HEIGHT = 340;
    private static final TitleColor DEFAULT_TITLE_COLOR = TitleColor.normal;
    private static final TitleSize DEFAULT_TITLE_SIZE = TitleSize.normal;
    private static final double DEFAULT_WIDTH = 0.333;

    private Logger log = LoggerFactory.getLogger(ReportPanel.class);

    /**
     * Initial total width (in pixels) occupied by the reports. This value is only used during the first rendering pass
     * of the ExtJs column layout, before the relative widths kick in.
     */
    private static final double INITIAL_ABSOLUTE_WIDTH = 1200;

    protected final IPluginContext context;
    protected final IPluginConfig config;

    public ReportPanel(final IPluginContext context, final IPluginConfig config) {
        super();
        this.context = context;
        this.config = config;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(REPORTS_PORTLET_JS));
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);

        final double columnWidth = config.getAsDouble(CONFIG_WIDTH, DEFAULT_WIDTH);
        final int width = (int)Math.round(columnWidth * INITIAL_ABSOLUTE_WIDTH);

        properties.put("columnWidth", columnWidth);
        properties.put("width", width);
        properties.put("height", config.getAsInteger(CONFIG_HEIGHT, DEFAULT_HEIGHT));

        final TitleColor titleColor = getTitleColor(config);
        final TitleSize titleSize = getTitleSize(config);
        properties.put("headerCssClass", "hippo-report-title-" + titleColor.name() + "-" + titleSize);

        final StringBuilder style = new StringBuilder("padding: 10px; margin: 10px; border-color: #b3b3b3;");

        if (config.getAsBoolean(CONFIG_BACKGROUND, DEFAULT_BACKGROUND)) {
            final RequestCycle rc = RequestCycle.get();
            style.append("background: url(");
            style.append(rc.urlFor(REPORT_PANEL_BG_IMAGE, null));
            style.append(") top repeat-x #ededed;");
        }

        properties.put("style", style.toString());
    }

    private TitleColor getTitleColor(IPluginConfig config) {
        final String titleColorName = config.getString(CONFIG_TITLE_COLOR);
        if (StringUtils.isNotBlank(titleColorName)) {
            try {
                return TitleColor.valueOf(titleColorName);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring unknown report title color '{}', known values are {}", titleColorName, Arrays.toString(TitleColor.values()));
            }
        }
        return DEFAULT_TITLE_COLOR;
    }

    private TitleSize getTitleSize(IPluginConfig config) {
        final String titleSizeName = config.getString(CONFIG_TITLE_SIZE);
        if (StringUtils.isNotBlank(titleSizeName)) {
            try {
                return TitleSize.valueOf(titleSizeName);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring unknown report title size '{}', known values are {}", titleSizeName, Arrays.toString(TitleSize.values()));
            }
        }
        return DEFAULT_TITLE_SIZE;
    }

    public String getResourceProviderKey() {
        return config.getString(ITranslateService.TRANSLATOR_ID);
    }

    @Override
    public String getString(Map<String, String> criteria) {
        String[] translators = config.getStringArray(ITranslateService.TRANSLATOR_ID);
        if (translators != null) {
            for (String translatorId : translators) {
                ITranslateService translator = context.getService(translatorId, ITranslateService.class);
                if (translator != null) {
                    String translation = translator.translate(criteria);
                    if (translation != null) {
                        return translation;
                    }
                }
            }
        }
        return null;
    }

}