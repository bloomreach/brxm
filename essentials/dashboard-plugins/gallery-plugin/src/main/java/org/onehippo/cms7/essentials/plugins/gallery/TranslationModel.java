/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugins.gallery;

import java.io.Serializable;

/**
 * @version "$Id$"
 */
public class TranslationModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private String language;
    private String message;
    private String property;

    public TranslationModel() {
    }

    public TranslationModel(final String language, final String message, final String property) {
        this.language = language;
        this.message = message;
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(final String property) {
        this.property = property;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TranslationModel{");
        sb.append("language=").append(language);
        sb.append(", message=").append(message);
        sb.append('}');
        return sb.toString();
    }


}
