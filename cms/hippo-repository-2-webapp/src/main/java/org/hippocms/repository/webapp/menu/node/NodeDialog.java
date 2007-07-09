/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.webapp.menu.node;

import org.apache.wicket.Component;
import org.hippocms.repository.webapp.menu.AbstractDialog;

public class NodeDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public NodeDialog(String id, Component targetComponent) {
        super(id, targetComponent);
        setTitle("Add a new Node");
        setCookieName(id);
    }
 

}
