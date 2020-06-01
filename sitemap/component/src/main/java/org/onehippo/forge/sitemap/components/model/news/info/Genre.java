/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components.model.news.info;

/**
 * Enum for supported values for the google news sitemap "genre" element
 * @author Wouter Danes
*/
@SuppressWarnings({"unused"})
public enum Genre {
    PRESS_RELEASE ("PressRelease"),
    SATIRE ("Satire"),
    BLOG ("Blog"),
    OPED ("OpEd"),
    OPINION ("Opinion"),
    USER_GENERATED ("UserGenerated");

    private String genre;

    Genre(String genre) {
        this.genre = genre;
    }

    @Override
    public String toString() {
        return genre;
    }
}
