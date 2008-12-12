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

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaDialogBehavior;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaDialogService;
import org.hippoecm.frontend.plugins.xinha.modal.imagepicker.ImageItemFactory.ImageItem;

public class ImagePickerBehavior extends XinhaDialogBehavior<XinhaImage> {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public ImagePickerBehavior(IPluginContext context, IPluginConfig config, String serviceId, JcrNodeModel nodeModel) {
        super(context, config, serviceId);
    }

    @Override
    protected String getServiceId() {
        return "cms-pickers/image";
    }

    @Override
    protected XinhaDialogService<XinhaImage> createXinhaDialogService() {
        return new XinhaImageDialogService();
    }

    public String onDialogOk() {
        return "{f_url: 'http://farm4.static.flickr.com/3239/3101529499_a7b22359bf_b.jpg'}";
    }

    public ImageItem insertImage(JcrNodeModel model) {
        return null;//dao.insertImageFromNodeModel(model);
    }

    
}
