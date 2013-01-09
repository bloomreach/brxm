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
package org.hippoecm.frontend.plugins.yui.webapp;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.hippoecm.frontend.plugins.yui.flash.FlashVersion;
import org.hippoecm.frontend.plugins.yui.flash.ProbeFlashBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.YuiContext;
import org.hippoecm.frontend.plugins.yui.header.YuiHeaderCache;
import org.onehippo.yui.YahooNamespace;

/**
 * This is the base behavior needed for developing Wicket applications using the YUI framework developed by Hippo.
 * 
 * <p>
 * It's most important feature is implementing the {@link IYuiManager} interface, enabling the 
 * {@link AbstractYuiBehavior} and {@link AbstractYuiAjaxBehavior} to retrieve {@link IYuiContext} instances, and thus 
 * should be added to the {@link Page} instance of your application (this is required).
 * </p>
 * <p>
 * It also exposes the possibility of pre-loading the stylesheets of the YUI CSS foundation: 
 * <a href="http://developer.yahoo.com/yui/reset/">reset</a>, <a href="http://developer.yahoo.com/yui/fonts/">fonts</a>, 
 * <a href="http://developer.yahoo.com/yui/grids/">grids</a>, <a href="http://developer.yahoo.com/yui/base/">base</a> 
 * and reset-fonts-grids stylesheets, as well as pre-loading Wicket-Ajax dependencies. This can be configured in the
 * {@link WebAppSettings}.
 * </p>
 * <p>
 * Upon the first load, it will probe the client for a flash version and store the result so other components
 * can query it.
 * </p>
 * 
 * @see IYuiManager
 */
public class WebAppBehavior extends AbstractBehavior implements IYuiManager {

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

    FlashVersion flash;

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
    public void bind(Component component) {
        super.bind(component);

        component.add(new ProbeFlashBehavior() {

            @Override
            protected void handleFlash(FlashVersion flash) {
                WebAppBehavior.this.flash = flash;
            }
        });

    }

    @Override
    public void renderHead(IHeaderResponse response) {
        headerContributor.renderHead(response);
        helper.renderHead(response);
    }

    public IYuiContext newContext() {
        return new YuiContext(headerContributor);
    }

    public FlashVersion getFlash() {
        return flash;
    }

    public void setFlash(FlashVersion flash) {
        this.flash = flash;
    }
}
