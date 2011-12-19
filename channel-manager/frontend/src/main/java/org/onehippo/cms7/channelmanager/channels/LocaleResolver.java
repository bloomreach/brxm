package org.onehippo.cms7.channelmanager.channels;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.repository.translation.HippoTranslationNodeType;

/**
 * Helper class to resolve the locale of a JCR node.
 */
class LocaleResolver implements IClusterable {

    private static final long serialVersionUID = 1L;
    private ILocaleProvider localeProvider;

    LocaleResolver(ILocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    /**
     * Retrieves the locale of a node.
     *
     * @param node a JCR node
     * @return the locale of the node, or null if no locale could be determined.
     * @throws RepositoryException when an unexpected error occurred while retrieving the locale of the node
     */
    Locale getLocale(Node node) throws RepositoryException {
        if (node.hasProperty(HippoTranslationNodeType.LOCALE)) {
            String localeName = node.getProperty(HippoTranslationNodeType.LOCALE).getString();
            return localeProvider.getLocale(localeName).getLocale();
        }
        return null;
    }

}
