/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.gallery.editor.crop;

import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.widget.WidgetBehavior;
import org.onehippo.yui.YahooNamespace;
import org.onehippo.yui.YuiNamespace;

public class ImageCropBehavior extends WidgetBehavior implements YuiNamespace {
    private static final long serialVersionUID = 1L;

    private static final CssResourceReference CROPPER_SKIN = new CssResourceReference(YahooNamespace.class, YahooNamespace.NS.getPath() +
            "imagecropper/assets/skins/sam/imagecropper-skin.css");
    private static final CssResourceReference RESIZE_SKIN = new CssResourceReference(YahooNamespace.class, YahooNamespace.NS.getPath() +
            "resize/assets/skins/sam/resize-skin.css");
    private static final CssResourceReference DIALOG_SKIN = new CssResourceReference(ImageCropBehavior.class, "crop-editor-dialog.css");


    public ImageCropBehavior(ImageCropSettings settings) {
        super(settings);
        getTemplate().setInstance("YAHOO.hippo.ImageCropper");
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(this, "hippoimagecropper");
        context.addCssReference(CROPPER_SKIN);
        context.addCssReference(RESIZE_SKIN);
        context.addCssReference(DIALOG_SKIN);

        super.addHeaderContribution(context);
    }

    @Override
    public String getPath() {
        return "";
    }
}
