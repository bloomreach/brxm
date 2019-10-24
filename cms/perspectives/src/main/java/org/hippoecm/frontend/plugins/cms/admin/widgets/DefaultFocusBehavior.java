/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;

/**
 * Focuses the component bound to this behavior after a page load or Ajax response. This behavior should be used
 * for one component only.
 */
public class DefaultFocusBehavior extends Behavior {

    @Override
    public void bind(final Component component) {
        component.setOutputMarkupId(true);
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        final String domReadyScript = String.format("document.getElementById('%s').focus();", component.getMarkupId());
        response.render(OnDomReadyHeaderItem.forScript(
                domReadyScript));
    }
}
