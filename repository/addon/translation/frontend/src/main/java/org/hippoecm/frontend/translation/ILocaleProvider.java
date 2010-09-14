/*
 *  Copyright 2010 Hippo.
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

public interface ILocaleProvider extends IClusterable {
    
    enum IconType {
        SMALL, SMALL_NEW
    }

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
        
        abstract public ResourceReference getIcon(IconType type);

        abstract public String getDisplayName(Locale locale);
    }

    List<HippoLocale> getLocales();

}
