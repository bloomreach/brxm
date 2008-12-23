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
package org.hippoecm.frontend.plugins.xinha.dialog.images;

import java.util.HashMap;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.IDialogModel;
import org.hippoecm.frontend.plugins.xinha.dialog.XinhaDialogBehavior;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImage;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImageService;

public class ImagePickerBehavior extends XinhaDialogBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public ImagePickerBehavior(IPluginContext context, IPluginConfig config, String serviceId) {
        super(context, config, serviceId);
    }

    @Override
    protected void configureModal(final ModalWindow modal) {
        super.configureModal(modal);
        modal.setInitialHeight(455);
        modal.setInitialWidth(850);
    }
    
    @Override
    protected IDialogModel newDialogModel(HashMap<String, String> p) {
        return getImageService().createXinhaImage(p);
    }

    @Override
    protected String getId() {
        return "imagepicker";
    }

    @Override
    protected void onOk(IDialogModel model) {
        XinhaImage xi = (XinhaImage) model;
        getImageService().attach(xi);
    }

    @Override
    protected void onRemove(IDialogModel model) {
        XinhaImage img = (XinhaImage) model;
        if (getImageService().detach(img)) {
            img.reset();
        }
    }

    private XinhaImageService getImageService() {
        return context.getService(clusterServiceId + ".images", XinhaImageService.class);
    }
}
