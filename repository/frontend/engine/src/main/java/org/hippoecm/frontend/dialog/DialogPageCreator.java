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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.hippoecm.frontend.dialog.lookup.LookupDialog;

public class DialogPageCreator implements PageCreator {

    private static final long serialVersionUID = 1L;
    private LookupDialog lookupDialog;

    public DialogPageCreator(LookupDialog lookupDialog) {
        this.lookupDialog = lookupDialog;
    }

    public Page createPage() {
        return lookupDialog;
    }

}
