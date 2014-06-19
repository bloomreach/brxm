/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest.model.gallery;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.Restful;


/**
 * @version "$Id$"
 */
@XmlRootElement(name = "imageGalleryData")
public class ImageGalleryDataRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private ImageProcessorRestful imageProcessor = null;
    private List<ImageSetRestful> imageSets = new ArrayList<>();

    public ImageGalleryDataRestful() {
    }

    public ImageGalleryDataRestful(final ImageProcessorRestful imageProcessor, final List<ImageSetRestful> imageSets) {
        this.imageProcessor = imageProcessor;
        this.imageSets = imageSets;
    }

    public ImageProcessorRestful getImageProcessor() {
        return imageProcessor;
    }

    public void setImageProcessor(final ImageProcessorRestful imageProcessor) {
        this.imageProcessor = imageProcessor;
    }

    public List<ImageSetRestful> getImageSets() {
        return imageSets;
    }

    public void setImageSets(final List<ImageSetRestful> imageSets) {
        this.imageSets = imageSets;
    }
}
