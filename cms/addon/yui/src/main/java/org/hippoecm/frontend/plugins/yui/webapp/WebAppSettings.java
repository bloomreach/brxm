/*
 *  Copyright 2009 Hippo.
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

import java.io.Serializable;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class WebAppSettings implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

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
