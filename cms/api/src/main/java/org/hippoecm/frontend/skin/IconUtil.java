/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.skin;

import java.io.IOException;

import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

class IconUtil {

    private IconUtil() {
    }

    static String svgAsString(PackageResourceReference reference) throws ResourceStreamNotFoundException, IOException {
        final PackageResource resource = reference.getResource();
        final IResourceStream resourceStream = resource.getResourceStream();
        if (resourceStream == null) {
            throw new NullPointerException("Failed to load SVG icon " + resource.toString());
        }
        String data = IOUtils.toString(resourceStream.getInputStream());
        //skip everything (comments, xml declaration and dtd definition) before <svg element
        return data.substring(data.indexOf("<svg "));
    }

}
