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
import java.util.Map;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalBehavior;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;

public class LinkPickerBehavior extends XinhaModalBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private JcrNodeModel nodeModel;

    public LinkPickerBehavior(XinhaModalWindow modalWindow, JcrNodeModel model) {
        super(modalWindow);
        nodeModel = model;
    }

    @Override
    protected XinhaContentPanel<XinhaLink> createContentPanel(Map<String, String> params) {
        EnumMap<XinhaLink, String> enums = new EnumMap<XinhaLink, String>(XinhaLink.class);
        for (XinhaLink xl : XinhaLink.values()) {
            enums.put(xl, params.get(xl.getValue()));
        }
        return new LinkPickerContentPanel(modalWindow, nodeModel, enums);
    }

}
