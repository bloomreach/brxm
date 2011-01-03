package org.hippoecm.hst.content.beans.standard;

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean mapping class for the 'hippogallery:image' document type
 */

@Node(jcrType = "hippogallery:image")
public class HippoGalleryImage extends HippoResource implements HippoGalleryImageBean{
    
    private static Logger log = LoggerFactory.getLogger(HippoGalleryImage.class);
    
    public int getHeight() {
        if(!getValueProvider().hasProperty("hippogallery:height")) {
            log.debug("no height property available for image '{}'. Return -1", getValueProvider().getPath());
            return -1;
        }
        return this.getValueProvider().getLong("hippogallery:height").intValue();
    }

    public int getWidth() {
        if(!getValueProvider().hasProperty("hippogallery:width")) {
            log.debug("no width property available for image '{}'. Return -1", getValueProvider().getPath());
            return -1;
        }
        return this.getValueProvider().getLong("hippogallery:width").intValue();
    }

}
