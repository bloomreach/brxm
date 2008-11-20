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
package org.hippoecm.tools.importer.gallery.mapping;

import org.apache.commons.configuration.Configuration;
import org.hippoecm.tools.importer.api.Content;
import org.hippoecm.tools.importer.api.ImportException;
import org.hippoecm.tools.importer.api.Mapper;
import org.hippoecm.tools.importer.api.Mapping;

/**
 * Filter for the content importers.
 */
public class GalleryMapper implements Mapper {

    final static String SVN_ID = "$Id$";

    public void setup(Configuration config) throws ImportException {
    }

    public Mapping map(final Content content) throws ImportException {
        final String path = content.getLocation();
        return new Mapping() {

            public String getNodeType() {
                return content.isFolder() ? "hippogallery:stdImageGallery" : "hippogallery:exampleImageSet";
            }

            public String getPath() {
                return path;
            }

        };
    }

}
