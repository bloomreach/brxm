/*
 * Copyright 2012-2023 Bloomreach
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
package org.onehippo.forge.sitemap.components.model;

/**
 * Enum that holds the different changefreq values for sitemaps
 * @author Wouter Danes
 */
@SuppressWarnings({"unused"})
public enum ChangeFrequency {

    ALWAYS("always"),
    HOURLY("hourly"),
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly"),
    NEVER("never");

    private final String description;

    ChangeFrequency(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
