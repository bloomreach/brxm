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

package org.hippoecm.hst.plugins.frontend;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.browse.AbstractBrowseView;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;

public class HstEditorPerspective extends Perspective {

    private static final long serialVersionUID = 1L;

    private final AbstractBrowseView browserView;

    public HstEditorPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        browserView = new AbstractBrowseView(context, config, null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("extension.list");
            }

            @Override
            protected void onBrowse() {
                focus(null);
            }
        };
    }

    @Override
    protected void onDetach() {
        browserView.detach();
        super.onDetach();
    }

}
