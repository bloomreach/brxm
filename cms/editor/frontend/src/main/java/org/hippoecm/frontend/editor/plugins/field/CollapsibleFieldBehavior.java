/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.model.ReadOnlyModel;

public class CollapsibleFieldBehavior extends AbstractDefaultAjaxBehavior {

    private boolean isCollapsed;
    private final String fieldSelector;

    public CollapsibleFieldBehavior(final String fieldSelector) {
        this.fieldSelector = fieldSelector;
    }

    @Override
    protected void onBind() {
        getComponent().add(ClassAttribute.append(ReadOnlyModel.of(() -> isCollapsed ? "collapsed" : "")));
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);

        final String toggleScript = String.format(
            "(function() {" +
            "  const field = $('%s');" +
            "  $('> .hippo-editor-field > .hippo-editor-field-title', field).on('click', function() {" +
            "    field.toggleClass('collapsed');" +
            "    %s" +
            "  });" +
            "})();",
            fieldSelector, getCallbackScript());
        response.render(OnDomReadyHeaderItem.forScript(toggleScript));
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        isCollapsed = !isCollapsed;
    }
}
