package org.onehippo.cms7.essentials.dashboard.model;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public enum DependencyType {


    INVALID(null), SITE("site"), CMS("cms"), BOOTSTRAP("bootstrap"), BOOTSTRAP_CONFIG("config"), BOOTSTRAP_CONTENT("content"), ESSENTIALS("essentials");
    private final String name;

    DependencyType(final String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DependencyType{");
        sb.append("name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static DependencyType typeForName(final String type) {
        if (Strings.isNullOrEmpty(type)) {
            return DependencyType.INVALID;
        }
        if (type.equals(SITE.name)) {
            return SITE;
        } else if (type.equals(CMS.name)) {
            return CMS;
        } else if (type.equals(BOOTSTRAP.name)) {
            return BOOTSTRAP;
        } else if (type.equals(BOOTSTRAP_CONFIG.name)) {
            return BOOTSTRAP_CONFIG;
        } else if (type.equals(BOOTSTRAP_CONTENT.name)) {
            return BOOTSTRAP_CONTENT;
        }
        return DependencyType.INVALID;

    }
}
