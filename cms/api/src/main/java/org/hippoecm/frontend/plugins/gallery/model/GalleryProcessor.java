/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.model;

import java.awt.Dimension;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.util.io.IClusterable;

public interface GalleryProcessor extends IClusterable {

    String GALLERY_PROCESSOR_ID = "gallery.processor.id";

    String DEFAULT_GALLERY_PROCESSOR_SERVICE_ID = "service.gallery.processor";

    void makeImage(Node node, InputStream istream, String mimeType, String filename) throws GalleryException,
            RepositoryException;

    /**
     * @deprecated As version 2.28.00, the resource validation is moved to
     * {@link org.hippoecm.frontend.plugins.yui.upload.validation.DefaultUploadValidationService}
     *
     */
    @Deprecated
    void validateResource(Node node, String fileName) throws GalleryException, RepositoryException;

    void initGalleryResource(Node node, InputStream data, String mimeType, String fileName, Calendar lastModified)
            throws GalleryException, RepositoryException;

    Dimension getDesiredResourceDimension(Node node) throws GalleryException, RepositoryException;

    boolean isUpscalingEnabled(Node node) throws GalleryException, RepositoryException;

    Map<String, ScalingParameters> getScalingParametersMap() throws RepositoryException;

}

