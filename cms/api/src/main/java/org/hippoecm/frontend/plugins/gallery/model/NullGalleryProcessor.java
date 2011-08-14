package org.hippoecm.frontend.plugins.gallery.model;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This gallery processor does not perform any additional processing on a gallery node.
 */
public class NullGalleryProcessor extends DefaultGalleryProcessor {

    @Override
    protected void makeThumbnailImage(final Node node, final InputStream resourceData, final String mimeType) throws RepositoryException, GalleryException {
        // do nothing
    }

    @Override
    protected void makeRegularImage(final Node node, final String name, final InputStream istream, final String mimeType, final Calendar lastModified) throws RepositoryException {
        // do nothing
    }

}
