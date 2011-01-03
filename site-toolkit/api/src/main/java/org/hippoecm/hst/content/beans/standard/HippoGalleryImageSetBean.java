package org.hippoecm.hst.content.beans.standard;

/**
 * The interface the default hippo gallery set impls must implement.
 */
public interface HippoGalleryImageSetBean extends HippoBean {

    /**
     * @return the filename of the {@link HippoGalleryImageSetBean} and <code>null</code> when not present
     */
    String getFileName();
    
    /**
     * @return the description of the {@link HippoGalleryImageSetBean}  and <code>null</code> when not present
     */
    String getDescription();
    
    /**
     * @return the thumbnail image of this {@link HippoGalleryImageSetBean} or <code>null</code> when it cannot be retrieved
     */
    HippoGalleryImageBean getThumbnail();

    /**
     * @return the original image of this {@link HippoGalleryImageSetBean} or <code>null</code> when it cannot be retrieved
     */
    HippoGalleryImageBean getOriginal();
}
