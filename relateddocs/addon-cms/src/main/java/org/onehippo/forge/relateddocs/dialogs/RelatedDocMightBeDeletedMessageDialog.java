/*
 *  Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.relateddocs.dialogs;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;

/**
 * An implementation of a dialog to show an information message to the user
 */
public class RelatedDocMightBeDeletedMessageDialog  extends AbstractDialog {

    public RelatedDocMightBeDeletedMessageDialog() {
        super();
        add(new Label("label", new ResourceModel("relateddoc-might-be-deleted-text")));
        setFocusOnOk();
        setCancelVisible(false);
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of(getString("relateddoc-might-be-deleted-title"));
    }

   @Override
    public IValueMap getProperties() {
       return DialogConstants.SMALL;
    }

}
