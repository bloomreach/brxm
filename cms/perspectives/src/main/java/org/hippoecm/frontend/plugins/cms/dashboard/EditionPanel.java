/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.dashboard;

import org.apache.wicket.markup.html.panel.Panel;

/**
 * Created with IntelliJ IDEA. User: mnour Date: 9/6/12 Time: 3:31 PM To change this template use File | Settings | File
 * Templates.
 */
public class EditionPanel extends Panel {

    private final String edition;

    /**
     * @see org.apache.wicket.Component#Component(String)
     */
    public EditionPanel(String id) {
        super(id);
        edition = "";
    }

    public EditionPanel(String id, String variation) {
        super(id);
        if (variation == null) {
            edition = "";
        } else {
            edition = variation;
        }
    }

    @Override
    public String getVariation() {
        return (edition != null) ? edition : "";
    }

}
