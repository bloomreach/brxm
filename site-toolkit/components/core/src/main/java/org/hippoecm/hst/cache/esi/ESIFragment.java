/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.esi;

import java.io.Serializable;

/**
 * ESIFragment
 * <P>
 * The real ESI fragment abstraction.
 * </P>
 */
public abstract class ESIFragment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The ESI fragment type. e.g., comment, include or remove */
    private ESIFragmentType type;

    /**
     * The real effective ESI fragment source.
     * Comment block fragment excludes the comment start ('<!--esi') and comment end ('-->'),
     * and vars tag fragment excludes the tag start and end, while
     * the others contain all the source string as is.
     */
    private String source;

    public ESIFragment(ESIFragmentType type, String source) {
        this.type = type;
        this.source = source;
    }

    public ESIFragmentType getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return new StringBuilder(super.toString()).append(" [").append(type).append("]: ").append(source).toString();
    }
}
