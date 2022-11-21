/*
 * Copyright 2008-2022 Bloomreach
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
package org.onehippo.addon.frontend.gallerypicker;

import java.io.Serializable;

/**
 * Factory class for {@link ImageItem} objects.
 */
public class ImageItemFactory implements Serializable {

    public ImageItem createImageItem() {
        return new ImageItem();
    }

    public ImageItem createImageItem(String uuid) {
        return new ImageItem(uuid);
    }
}
