/*
 *  Copyright 2019-2023 Bloomreach
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
package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.InputStream;

public class ImageOperationResult {

    private InputStream data;
    private int width;
    private int height;

    public ImageOperationResult() {
    }

    public ImageOperationResult(final InputStream data, final int width, final int height) {
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public InputStream getData() {
        return data;
    }

    public void setData(final InputStream data) {
        this.data = data;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(final int height) {
        this.height = height;
    }
}
