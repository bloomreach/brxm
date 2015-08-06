package org.onehippo.repository.l10n;

import java.util.Locale;

public interface ResourceBundle {

    Locale getLocale();

    String getName();

    String getString(String key);

}
