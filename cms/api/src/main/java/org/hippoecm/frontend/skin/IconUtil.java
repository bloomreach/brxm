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
