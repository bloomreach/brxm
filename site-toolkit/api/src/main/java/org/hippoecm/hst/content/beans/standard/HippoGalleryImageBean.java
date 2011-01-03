package org.hippoecm.hst.content.beans.standard;

/**
 * The interface which all hippo gallery image implementations should implement
 *
 */
public interface HippoGalleryImageBean extends HippoResourceBean {
    /** 
     * @return the height of the image: if their is no height property available, -1 is returned
     */
    int getHeight();
    
    /**
     * @return the width of the image: if their is no widht property available, -1 is returned
     */
    int getWidth(); 
}
