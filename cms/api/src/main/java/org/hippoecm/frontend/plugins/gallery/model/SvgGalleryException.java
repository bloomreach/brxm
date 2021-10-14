package org.hippoecm.frontend.plugins.gallery.model;

import org.hippoecm.frontend.validation.SvgValidationResult;

public class SvgGalleryException extends GalleryException {

    private final SvgValidationResult validationResult;

    public SvgGalleryException(final String message, final SvgValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }

    public SvgGalleryException(final String message, final Throwable cause,
                                     final SvgValidationResult validationResult) {
        super(message, cause);
        this.validationResult = validationResult;
    }

    public SvgValidationResult getValidationResult() {
        return validationResult;
    }
}
