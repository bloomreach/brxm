package org.onehippo.cms7.essentials.dashboard.utils.code.exc;

/**
 * @version "$Id: EssentialsCodeCreationException.java 171335 2013-07-23 08:49:55Z mmilicevic $"
 */
public class EssentialsCodeCreationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EssentialsCodeCreationException() {
    }

    public EssentialsCodeCreationException(final String message) {
        super(message);
    }

    public EssentialsCodeCreationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EssentialsCodeCreationException(final Throwable cause) {
        super(cause);
    }

    public EssentialsCodeCreationException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
