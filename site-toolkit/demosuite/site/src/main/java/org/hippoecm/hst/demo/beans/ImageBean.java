package org.hippoecm.hst.demo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoItem;
import org.hippoecm.hst.content.beans.standard.HippoResource;


/**
 * Bean mapping class for the 'hippogallery:exampleImageSet' document type
 *
 * @author Roberto van der Linden
 * @author Jeroen Reijn
 */
@Node(jcrType = "hippogallery:exampleImageSet")
public class ImageBean extends HippoItem implements HippoBean {

    /**
     * Get the thumbnail version of the image.
     *
     * @return the thumbnail version of the image
     */
    public HippoResource getThumbnail() {
        return getBean("hippogallery:thumbnail");
    }

    /**
     * Get the picture version of the image.
     *
     * @return the picture version of the image
     */
    public HippoResource getPicture() {
        return getBean("hippogallery:picture");
    }

}
