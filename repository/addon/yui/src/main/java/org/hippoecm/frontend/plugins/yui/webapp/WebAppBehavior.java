/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.yui.webapp;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.YuiContext;
import org.hippoecm.frontend.plugins.yui.header.YuiHeaderCache;
import org.onehippo.yui.YahooNamespace;

public class WebAppBehavior extends AbstractBehavior implements IYuiManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final CompressedResourceReference RESET_CSS = new CompressedResourceReference(YahooNamespace.class,
            YahooNamespace.NS.getPath() + "reset/reset-min.css");
    private static final CompressedResourceReference FONTS_CSS = new CompressedResourceReference(YahooNamespace.class,
            YahooNamespace.NS.getPath() + "fonts/fonts-min.css");
    private static final CompressedResourceReference GRIDS_CSS = new CompressedResourceReference(YahooNamespace.class,
            YahooNamespace.NS.getPath() + "grids/grids-min.css");

    private static final CompressedResourceReference BASE_CSS = new CompressedResourceReference(YahooNamespace.class,
            YahooNamespace.NS.getPath() + "base/base-min.css");
    private static final CompressedResourceReference RESET_FONTS_GRIDS_CSS = new CompressedResourceReference(
            YahooNamespace.class, YahooNamespace.NS.getPath() + "reset-fonts-grids/reset-fonts-grids.css");

    YuiHeaderCache headerContributor;
    YuiContext helper;

    public WebAppBehavior(WebAppSettings settings) {
        headerContributor = new YuiHeaderCache(settings.isLoadWicketAjax());
        helper = new YuiContext(headerContributor);
        if (settings.isLoadResetFontsGrids()) {
            helper.addCssReference(RESET_FONTS_GRIDS_CSS);
        } else {
            if (settings.isLoadCssReset()) {
                helper.addCssReference(RESET_CSS);
            }
            if (settings.isLoadFonts()) {
                helper.addCssReference(FONTS_CSS);
            }
            if (settings.isLoadCssGrids()) {
                helper.addCssReference(GRIDS_CSS);
            }
        }
        if (settings.isLoadCssBase()) {
            helper.addCssReference(BASE_CSS);
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        headerContributor.renderHead(response);
        helper.renderHead(response);
    }

    public IYuiContext newContext() {
        return new YuiContext(headerContributor);
    }

}
