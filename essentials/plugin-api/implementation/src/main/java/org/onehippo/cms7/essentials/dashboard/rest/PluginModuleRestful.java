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

package org.onehippo.cms7.essentials.dashboard.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.Restful;

import com.wordnik.swagger.annotations.ApiModel;

/**
 * Plugin javascript module descriptor.
 * Contains application name and plugin(s) javascript references.
 *
 * @version "$Id$"
 */

@ApiModel
@XmlRootElement(name = "module")
public class PluginModuleRestful implements Restful {

    private static final long serialVersionUID = 1L;


    /**
     * Contains a list of all include libraries
     */


    public static final String DEFAULT_APP_NAME = "hippo.essentials";
    /**
     * Name fo module application,
     */
    private String application = DEFAULT_APP_NAME;


    private Map<String, PrefixedLibrary> includes = new HashMap<>();


    public void addLibrary(final String name, final PrefixedLibrary library) {

        includes.put(name, library);

    }


    public Map<String, PrefixedLibrary> getIncludes() {
        return includes;
    }

    public void setIncludes(final Map<String, PrefixedLibrary> includes) {
        this.includes = includes;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(final String application) {
        this.application = application;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginModuleRestful{");
        sb.append("includes=").append(includes);
        sb.append(", application='").append(application).append('\'');
        sb.append('}');
        return sb.toString();
    }


    //############################################
    // HELPER CLASSES
    //############################################



    public static class PrefixedLibrary {

        private String prefix;

        public PrefixedLibrary(final String prefix) {
            this.prefix = prefix;
        }

        public PrefixedLibrary() {
        }

        private List<Library> items = new ArrayList<>();

        public void addLibrary(final Library library) {

            items.add(library);

        }

        public List<Library> getItems() {
            return items;
        }

        public void setItems(final List<Library> items) {
            this.items = items;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }
    }

    public static class Library {
        private String browser;
        private String component;
        private String library;


        public Library(final String component, final String library, final String browser) {
            this.component = component;
            this.library = library;
            this.browser = browser;
        }

        public Library(final String component, final String library) {
            this.component = component;
            this.library = library;
        }

        public Library() {
        }

        public String getBrowser() {
            return browser;
        }

        public void setBrowser(final String browser) {
            this.browser = browser;
        }

        public String getComponent() {
            return component;
        }

        public void setComponent(final String component) {
            this.component = component;
        }

        public String getLibrary() {
            return library;
        }

        public void setLibrary(final String library) {
            this.library = library;
        }
    }


}
