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
package org.hippoecm.frontend.service;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.markup.html.link.PopupSettings;

/**
 * Opens a popup window for a URL.
 */
public interface IPopupService extends IClusterable {

    final static int DEFAULT_POPUP_SETTINGS =
                    PopupSettings.RESIZABLE
                    | PopupSettings.SCROLLBARS
                    | PopupSettings.LOCATION_BAR
                    | PopupSettings.MENU_BAR
                    | PopupSettings.TOOL_BAR;

    /**
     * Opens a popup window for a URL.
     *
     * @param popupSettings
     *
     * @param url the URL to open
     */
    public void openPopupWindow(PopupSettings popupSettings, String url);

}
