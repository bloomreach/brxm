package org.onehippo.cms7.channelmanager.channels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to resolve the locale of a JCR node.
 */
class LocaleResolver implements IClusterable {

    private static Logger log = LoggerFactory.getLogger(LocaleResolver.class);
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
            ILocaleProvider.HippoLocale hippoLocale = localeProvider.getLocale(localeName);
            if (hippoLocale != null) {
                return hippoLocale.getLocale();
            } else {
                log.warn("The property '{}' of node '{}' contains an unknown locale name '{}'. Known locale names are {}",
                        new String[]{HippoTranslationNodeType.LOCALE, node.getPath(), localeName, getAllLocaleNames().toString()});
            }
        }
        return null;
    }

    private Collection<String> getAllLocaleNames() {
        List<? extends ILocaleProvider.HippoLocale> all = localeProvider.getLocales();
        ArrayList result = new ArrayList<String>(all.size());
        for (ILocaleProvider.HippoLocale hippoLocale : localeProvider.getLocales()) {
            result.add(hippoLocale.getName());
        }
        return result;
    }

}
