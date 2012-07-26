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

/**
 * Setting for {@link WebAppbehavior}.
 * 
 * Wicket-Ajax is loaded by default.
 */
public class WebAppSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean loadWicketAjax = true; //load Wicket-Ajax by default
    private boolean loadReset = false;
    private boolean loadFonts = false;
    private boolean loadGrids = false;
    private boolean loadBase = false;

    public WebAppSettings() {
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

    public boolean isLoadCssBase() {
        return loadBase;
    }

    public void setLoadCssBase(boolean loadBase) {
        this.loadBase = loadBase;
    }

    public boolean isLoadCssReset() {
        return loadReset;
    }

    public void setLoadCssReset(boolean loadReset) {
        this.loadReset = loadReset;
    }

    public boolean isLoadFonts() {
        return loadFonts;
    }

    public void setLoadCssFonts(boolean loadFonts) {
        this.loadFonts = loadFonts;
    }

    public boolean isLoadCssGrids() {
        return loadGrids;
    }

    public void setLoadCssGrids(boolean loadGrids) {
        this.loadGrids = loadGrids;
    }

}
