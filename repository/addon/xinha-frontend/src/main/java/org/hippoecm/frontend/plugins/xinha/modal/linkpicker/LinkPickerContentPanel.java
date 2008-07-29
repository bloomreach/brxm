/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.xinha.modal.linkpicker;

import java.util.EnumMap;

import org.apache.wicket.markup.html.form.TextField;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;

public class LinkPickerContentPanel extends XinhaContentPanel<XinhaLink> {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id:$";

    private static final long serialVersionUID = 1L;

    public LinkPickerContentPanel(XinhaModalWindow modal, JcrNodeModel nodeModel, EnumMap<XinhaLink, String> values) {
        super(modal, values);

        form.add(new TextField("url", newEnumModel(XinhaLink.HREF)));
    }

    @Override
    protected String getXinhaParameterName(XinhaLink k) {
        return k.getValue();
    }

}
