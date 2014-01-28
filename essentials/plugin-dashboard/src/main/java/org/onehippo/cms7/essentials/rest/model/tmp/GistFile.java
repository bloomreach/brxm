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

package org.onehippo.cms7.essentials.rest.model.tmp;

import java.io.Serializable;

/**
 * @version "$Id$"
 */

/**
 * Gist file class.
 */
public class GistFile implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 2067939890126207032L;

    private int size;

    private String content;

    private String filename;

    private String rawUrl;

    /**
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * @param size
     * @return this gist file
     */
    public GistFile setSize(int size) {
        this.size = size;
        return this;
    }

    /**
     * @return content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content
     * @return this gist file
     */
    public GistFile setContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * @return filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename
     * @return this gist file
     */
    public GistFile setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    /**
     * @return rawUrl
     */
    public String getRawUrl() {
        return rawUrl;
    }

    /**
     * @param rawUrl
     * @return this gist file
     */
    public GistFile setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
        return this;
    }
}
