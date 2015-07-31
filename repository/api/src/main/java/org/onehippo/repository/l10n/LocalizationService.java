package org.onehippo.repository.l10n;

import java.util.Locale;

import org.onehippo.cms7.services.SingletonService;

@SingletonService
public interface LocalizationService {

    ResourceBundle getResourceBundle(final String name, final Locale locale);

}
