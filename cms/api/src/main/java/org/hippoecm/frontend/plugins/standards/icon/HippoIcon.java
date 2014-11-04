package org.hippoecm.frontend.plugins.standards.icon;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.image.InlineSvgImage;
import org.hippoecm.frontend.service.IconSize;

public class HippoIcon extends Panel {
    
    // For now we don't inline SVG
    private boolean inlineSvg;
    
    public HippoIcon(final String id, final ResourceReference reference, final IconSize size) {
        super(id);
        
        setRenderBodyOnly(true);

        Fragment fragment;
        if (reference.getExtension().equalsIgnoreCase("svg") && inlineSvg) {
            fragment = new  Fragment ("container", "svgFragment", this);
            fragment.add(new InlineSvgImage("svg", reference));
        } else {
            fragment = new  Fragment ("container", "imageFragment", this);
            Image image = new CachingImage("image", reference);
            image.add(AttributeModifier.replace("width", size.getSize()));
            image.add(AttributeModifier.replace("height", size.getSize()));
            fragment.add(image);
        }

        fragment.setRenderBodyOnly(true);
        add(fragment);
    }
}
