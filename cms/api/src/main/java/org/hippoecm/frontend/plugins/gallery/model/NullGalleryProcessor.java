/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.model;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This gallery processor does not perform any additional processing on a gallery node.
 */
public class NullGalleryProcessor extends DefaultGalleryProcessor {

    private static final long serialVersionUID = 1L;

    @Override
    protected void makeThumbnailImage(final Node node, final InputStream resourceData, final String mimeType) throws RepositoryException, GalleryException {
        // do nothing
    }

    @Override
    protected void makeRegularImage(final Node node, final String name, final InputStream istream, final String mimeType, final Calendar lastModified) throws RepositoryException {
        // do nothing
    }

    @Override
    public void validateResource(final Node node, final String fileName) throws GalleryException, RepositoryException {
        // skip validation, means any type of file can be uploaded
    }
}
