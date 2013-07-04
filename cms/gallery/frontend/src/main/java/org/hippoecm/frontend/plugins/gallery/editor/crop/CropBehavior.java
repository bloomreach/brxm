/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.editor.crop;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.onehippo.yui.YahooNamespace;

public class CropBehavior extends AbstractYuiBehavior {

    private static final CssResourceReference CROPPER_SKIN = new CssResourceReference(YahooNamespace.class, YahooNamespace.NS.getPath() + "imagecropper/assets/skins/sam/imagecropper-skin.css");
    private static final CssResourceReference RESIZE_SKIN = new CssResourceReference(YahooNamespace.class, YahooNamespace.NS.getPath() + "resize/assets/skins/sam/resize-skin.css");
    private static final CssResourceReference DIALOG_SKIN = new CssResourceReference(CropBehavior.class, "crop-editor-dialog.css");

    private String regionInputId;
    private String imagePreviewContainerId;
    private Dimension originalImageDimension;
    private Dimension thumbnailDimension;
    private boolean isUpscalingEnabled;


    public CropBehavior(String regionInputId, String imagePreviewContainerId, Dimension originalImageDimension, Dimension thumbnailDimension, boolean isUpscalingEnabled){
        this.regionInputId = regionInputId;
        this.imagePreviewContainerId = imagePreviewContainerId;
        this.originalImageDimension = originalImageDimension;
        this.thumbnailDimension = thumbnailDimension;
        this.isUpscalingEnabled = isUpscalingEnabled;
    }

    @Override
    public void bind(final Component component) {
        super.bind(component);
        component.setOutputMarkupId(true);
    }

    @Override
    public void addHeaderContribution(IYuiContext context)  {
        context.addModule(YahooNamespace.NS, "imagecropper");
        context.addOnDomLoad(new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                return getInitString();
            }
        });
        context.addCssReference(CROPPER_SKIN);
        context.addCssReference(RESIZE_SKIN);
        context.addCssReference(DIALOG_SKIN);
    }


    private String getInitString() {
        PackageTextTemplate cropperJsTemplate = new PackageTextTemplate(CropBehavior.class, "Hippo.ImageCropper.js");
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("originalImageMarkupId", getComponent().getMarkupId());
        parameters.put("imagePreviewContainerMarkupId", imagePreviewContainerId);
        parameters.put("regionInputMarkupId", regionInputId);
        parameters.put("originalImageWidth", originalImageDimension.getWidth());
        parameters.put("originalImageHeight", originalImageDimension.getHeight());
        parameters.put("thumbnailWidth", thumbnailDimension.getWidth());
        parameters.put("thumbnailHeight", thumbnailDimension.getHeight());
        parameters.put("isPreviewVisible", thumbnailDimension.getWidth() <= 200);
        parameters.put("isUpscalingEnabled", isUpscalingEnabled);

        return cropperJsTemplate.interpolate(parameters).getString();
    }


}

