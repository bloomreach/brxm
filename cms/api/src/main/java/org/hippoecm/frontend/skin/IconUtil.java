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
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

public class IconUtil {

    private IconUtil() {
    }

    public static String svgAsString(PackageResourceReference reference, String... cssClasses) throws ResourceStreamNotFoundException, IOException {
        final PackageResource resource = reference.getResource();
        final IResourceStream resourceStream = resource.getResourceStream();
        if (resourceStream == null) {
            throw new ResourceStreamNotFoundException("Cannot find SVG icon " + resource);
        }
        String svgAsString = IOUtils.toString(resourceStream.getInputStream());

        int rootIndex = svgAsString.indexOf("<svg");
        if (rootIndex == -1) {
            throw new IllegalArgumentException("Cannot find SVG root element in " + resource);
        }

        //skip everything (comments, xml declaration and dtd definition) before <svg element
        svgAsString = svgAsString.substring(rootIndex);

        //append css classes if present
        final String cssClassesAsString = cssClassesAsString(cssClasses);
        if (StringUtils.isNotEmpty(cssClassesAsString)) {
            //check if class attribute is present and part of <svg element
            final int classAttributeIndex = svgAsString.indexOf("class=\"");
            if (classAttributeIndex > -1 && classAttributeIndex < svgAsString.indexOf(">")) {
                int insertCssClassesAt = classAttributeIndex + 7;
                svgAsString = svgAsString.substring(0, insertCssClassesAt) + cssClassesAsString + " " +
                        svgAsString.substring(insertCssClassesAt);
            } else {
                svgAsString = "<svg class=\"" + cssClassesAsString + "\"" + svgAsString.substring(4);
            }
        }

        return svgAsString;
    }

    public static String cssClassesAsString(String... cssClasses) {
        if (ArrayUtils.isEmpty(cssClasses)) {
            return StringUtils.EMPTY;
        }

        return Arrays.stream(cssClasses).filter(StringUtils::isNotBlank).collect(Collectors.joining(" "));
    }

}
