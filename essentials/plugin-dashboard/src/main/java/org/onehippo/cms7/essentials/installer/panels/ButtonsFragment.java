/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.installer.panels;

import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class ButtonsFragment extends Fragment {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ButtonsFragment.class);

    public ButtonsFragment(final String id, final String markupId, final MenuPanel provider, final MarkupContainer markupProvider, final List<Plugin> pluginList, final List<Plugin> mainPlugins) {
        super(id, markupId, provider);
        setOutputMarkupId(true);

        final AjaxLink<Void> buttonTools = new AjaxLink<Void>("buttonTools") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                provider.swapFragment(target);
            }
        };
        final AjaxLink<Void> buttonPlugins = new

                AjaxLink<Void>("buttonPlugins") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        provider.pluginSelected(target, pluginList, mainPlugins);
                        // provider.swapFragment(target);
                    }
                };
        add(buttonTools);
        add(buttonPlugins);

    }
}
