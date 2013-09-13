/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.gallery.model;

import java.io.Serializable;

/**
 * @version "$Id: TranslationModel.java 163479 2013-05-06 09:19:11Z mmilicevic $"
 */
public class TranslationModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private String language;
    private String message;

    public TranslationModel() {
    }

    public TranslationModel(final String language, final String message) {
        this.language = language;
        this.message = message;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TranslationModel{");
        sb.append("language=").append(language);
        sb.append(", message=").append(message);
        sb.append('}');
        return sb.toString();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }
}
