/*
 * Copyright 2009-2023 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.hippoecm.addon.workflow;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;

public abstract class MenuDescription extends Panel {

    private static final long serialVersionUID = 1L;

    public MenuDescription() {
        super("menu");
    }

    /**
     * Returns the label to be used to render the menu button.
     * Should have the wicket id 'label'.
     */
    public abstract Component getLabel();

    public MarkupContainer getContent() {
        return null;
    }
}
