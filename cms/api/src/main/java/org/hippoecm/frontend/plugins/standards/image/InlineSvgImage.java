package org.hippoecm.frontend.plugins.standards.image;

import java.io.IOException;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InlineSvgImage extends WebComponent {
    
    public static final Logger log = LoggerFactory.getLogger(InlineSvgImage.class);
    
    PackageResourceReference reference;
    
    public InlineSvgImage(final String id, final ResourceReference reference) {
        super(id);
        if (reference instanceof PackageResourceReference) {
            this.reference = (PackageResourceReference) reference;   
        } else {
            this.reference = new PackageResourceReference(reference.getKey());
        }
        setRenderBodyOnly(true);
    }

    @Override
    protected void onComponentTag(final ComponentTag tag) {
        try {
            String data = IOUtils.toString(reference.getResource().getResourceStream().getInputStream());
            RequestCycle.get().getResponse().write(data);
        } catch (IOException | ResourceStreamNotFoundException e) {
            log.error("Failed to load svg image[" + reference.getName() + "]", e);
        }

        super.onComponentTag(tag);
    }
}
