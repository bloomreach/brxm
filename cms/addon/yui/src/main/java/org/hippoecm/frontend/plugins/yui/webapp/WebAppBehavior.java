package org.hippoecm.frontend.plugins.yui.webapp;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.header.YuiHeaderContributor;
import org.hippoecm.frontend.plugins.yui.header.YuiHeaderContributor.YuiContext;
import org.onehippo.yui.YahooNamespace;

public class WebAppBehavior extends AbstractBehavior {
    private static final long serialVersionUID = 1L;

    private static final CompressedResourceReference RESET_CSS = new CompressedResourceReference(YahooNamespace.class,
            YahooNamespace.NS.getPath() + "reset/reset-min.css");
    private static final CompressedResourceReference FONTS_CSS = new CompressedResourceReference(YahooNamespace.class, 
            YahooNamespace.NS.getPath() + "fonts/fonts-min.css");
    private static final CompressedResourceReference GRIDS_CSS = new CompressedResourceReference(YahooNamespace.class, 
            YahooNamespace.NS.getPath() + "grids/grids-min.css");

    private static final CompressedResourceReference BASE_CSS = new CompressedResourceReference(YahooNamespace.class, 
            YahooNamespace.NS.getPath() + "base/base-min.css");
    private static final CompressedResourceReference RESET_FONTS_GRIDS_CSS = new CompressedResourceReference(YahooNamespace.class,
            YahooNamespace.NS.getPath() + "reset-fonts-grids/reset-fonts-grids.css");

    
    YuiHeaderContributor headerContributor;
    YuiContext helper;

    public WebAppBehavior(WebAppSettings settings) {
        headerContributor = new YuiHeaderContributor(settings.isLoadWicketAjax());
        helper = headerContributor.new YuiContext();
        if(settings.isLoadResetFontsGrids()) {
            helper.addCssReference(RESET_FONTS_GRIDS_CSS);
        } else {
            if (settings.isLoadReset()) {
                helper.addCssReference(RESET_CSS);
            }
            if (settings.isLoadFonts()) {
                helper.addCssReference(FONTS_CSS);
            }
            if (settings.isLoadGrids()) {
                helper.addCssReference(GRIDS_CSS);
            }
        }
        if (settings.isLoadBase()) {
            helper.addCssReference(BASE_CSS);
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        headerContributor.renderHead(response);
        helper.renderHead(response);
    }

}

class WebAppSettings {
    private boolean loadWicketAjax = true; //load Wicket-Ajax by default
    private boolean loadReset = false;
    private boolean loadFonts = false;
    private boolean loadGrids = false;
    private boolean loadBase = false;

    public WebAppSettings(IPluginConfig config) {
        if (config.containsKey("load.wicket.ajax")) {
            loadWicketAjax = config.getBoolean("load.wicket.ajax");
        }
        if (config.containsKey("load.css.reset")) {
            loadReset = config.getBoolean("load.css.reset");
        }
        if (config.containsKey("load.css.fonts")) {
            loadFonts = config.getBoolean("load.css.fonts");
        }
        if (config.containsKey("load.css.grids")) {
            loadGrids = config.getBoolean("load.css.grids");
        }
        if (config.containsKey("load.css.base")) {
            loadBase = config.getBoolean("load.css.base");
        }
    }

    public boolean isLoadResetFontsGrids() {
        return loadReset && loadFonts && loadGrids;
    }

    public boolean isLoadWicketAjax() {
        return loadWicketAjax;
    }

    public void setLoadWicketAjax(boolean loadWicketAjax) {
        this.loadWicketAjax = loadWicketAjax;
    }

    public boolean isLoadBase() {
        return loadBase;
    }

    public void setLoadBase(boolean loadBase) {
        this.loadBase = loadBase;
    }
    
    public boolean isLoadReset() {
        return loadReset;
    }

    public void setLoadReset(boolean loadReset) {
        this.loadReset = loadReset;
    }
    
    public boolean isLoadFonts() {
        return loadFonts;
    }

    public void setLoadFonts(boolean loadFonts) {
        this.loadFonts = loadFonts;
    }

    public boolean isLoadGrids() {
        return loadGrids;
    }

    public void setLoadGrids(boolean loadGrids) {
        this.loadGrids = loadGrids;
    }
    
}