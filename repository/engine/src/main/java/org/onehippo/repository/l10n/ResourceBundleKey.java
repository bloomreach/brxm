package org.onehippo.repository.l10n;

import java.util.Locale;

class ResourceBundleKey {

    private final String name;
    private final Locale locale;

    ResourceBundleKey(final String name, final Locale locale) {
        this.name = name;
        this.locale = locale;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ResourceBundleKey that = (ResourceBundleKey) o;

        if (!name.equals(that.name)) {
            return false;
        }
        return locale.equals(that.locale);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + locale.hashCode();
        return result;
    }
}
