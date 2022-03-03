/*
 *  Copyright 2020-2022 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;

public class CollapsibleFieldTitle extends FieldTitle {

    private boolean isCollapsed;

    public CollapsibleFieldTitle(final String id, final IModel<String> captionModel, final IModel<String> hintModel, final boolean isRequired, final Component collapsingComponent) {
        this(id, captionModel, hintModel, isRequired, collapsingComponent, false);
    }

    public CollapsibleFieldTitle(final String id, final IModel<String> captionModel, final IModel<String> hintModel, final boolean isRequired, final Component collapsingComponent, final boolean isCollapsed) {
        super(id, captionModel, hintModel, isRequired);

        this.isCollapsed = isCollapsed;

        final IModel<String> collapsedModel = () -> isCollapsed ? "collapsed" : StringUtils.EMPTY;
        h3.add(ClassAttribute.append(collapsedModel));

        final HippoIcon expandCollapseIcon = HippoIcon.fromSprite("expand-collapse-icon", Icon.CHEVRON_DOWN);
        expandCollapseIcon.addCssClass("expand-collapse-icon");
        h3.add(expandCollapseIcon);

        if (collapsingComponent != null) {
            collapsingComponent.add(ClassAttribute.append("collapsible"), ClassAttribute.append(collapsedModel));

            final String labelSelector = "#" + h3.getMarkupId();
            final String fieldSelector = "#" + collapsingComponent.getMarkupId();
            h3.add(new CollapseBehavior(labelSelector, fieldSelector));
        }
    }

    protected void onCollapse(final boolean isCollapsed) {
    }

    private class CollapseBehavior extends AbstractDefaultAjaxBehavior {

        private final String labelSelector;
        private final String fieldSelector;

        public CollapseBehavior(final String labelSelector, final String fieldSelector) {
            this.labelSelector = labelSelector;
            this.fieldSelector = fieldSelector;
        }

        @Override
        public void renderHead(final Component component, final IHeaderResponse response) {
            super.renderHead(component, response);

            final String toggleScript = String.format(
                    "(function() {" +
                    "  const label = $('%s');" +
                    "  const field = $('%s');" +
                    "  label.on('click', function() {" +
                    "    label.toggleClass('collapsed');" +
                    "    field.toggleClass('collapsed');" +
                    "    %s" +
                    "  });" +
                    "})();",
                    labelSelector, fieldSelector, getCallbackScript());
            response.render(OnDomReadyHeaderItem.forScript(toggleScript));
        }

        @Override
        protected void respond(final AjaxRequestTarget target) {
            isCollapsed = !isCollapsed;
            onCollapse(isCollapsed);
        }

    }
}
