/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.translation;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.service.IconSize;

/**
 * Provider of content locale information.  Available as a service in the CMS,
 * it can be used by plugins to retrieve Locale and presentation
 * information.
 * <p>
 * The provider is made available under the "locale.id" configuration key.
 * When this key is not present, the fully qualified class name can be used
 * as a fall back.
 * <p>
 * Whereas the repository only depends on the name of a HippoLocale, the CMS
 * couples this name to a Locale for presentation purposes.
 */
public interface ILocaleProvider extends IClusterable {

    String SERVICE_ID = "locale.id";

    enum LocaleState {
        AVAILABLE, EXISTS, DOCUMENT, FOLDER, FOLDER_OPEN
    }

    /**
     * Description of a content locale.  Provides the (java) Locale and
     * UI presentation details.
     */
    abstract class HippoLocale implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Locale locale;
        private final String name;

        public HippoLocale(Locale locale, String name) {
            this.locale = locale;
            this.name = name;
        }

        public Locale getLocale() {
            return locale;
        }

        public String getName() {
            return name;
        }

        abstract public ResourceReference getIcon(IconSize size, LocaleState type);

        abstract public String getDisplayName(Locale locale);
    }

    List<? extends HippoLocale> getLocales();

    HippoLocale getLocale(String name);

}
