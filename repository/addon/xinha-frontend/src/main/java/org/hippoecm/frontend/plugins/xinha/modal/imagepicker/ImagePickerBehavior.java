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
package org.hippoecm.frontend.plugins.xinha.modal.imagepicker;

import java.util.EnumMap;
import java.util.Map;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalBehavior;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;

public class ImagePickerBehavior extends XinhaModalBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private ImageItemDAO imageItemDAO;

    public ImagePickerBehavior(XinhaModalWindow modalWindow, JcrNodeModel nodeModel) {
        super(modalWindow);
        imageItemDAO = new ImageItemDAO(nodeModel);
    }

    @Override
    protected XinhaContentPanel<XinhaImage> createContentPanel(Map<String, String> params) {
        EnumMap<XinhaImage, String> enums = new EnumMap<XinhaImage, String>(XinhaImage.class);
        for (XinhaImage p : XinhaImage.values()) {
            enums.put(p, params.get(p.getValue()));
        }
        return new ImagePickerContentPanel(modalWindow, enums, imageItemDAO);
    }

    public ImageItemDAO getImageItemDAO() {
        return imageItemDAO;
    }
}
