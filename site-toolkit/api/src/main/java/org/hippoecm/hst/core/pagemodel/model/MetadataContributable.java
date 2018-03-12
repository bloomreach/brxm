/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.pagemodel.model;

import java.util.Map;

/**
 * Interface for an abstraction to be able to contribute metadata.
 */
public interface MetadataContributable {

    /**
     * Return unmodifiable metadata map.
     * @return unmodifiable metadata map
     */
    Map<String, Object> getMetadataMap();

    /**
     * Put a metadata, {@code value}, by the {@code name}.
     * @param name metadata name
     * @param value metadata value
     */
    void putMetadata(String name, Object value);

    /**
     * Get the metadata value by the {@code name}, or null if not found.
     * @param name metadata name
     * @return metadata value, or null if not found.
     */
    Object getMetadata(String name);

    /**
     * Remove the existing metadata value by the {@code name} and return the old metadata value if removed.
     * Or null if nothing was removed.
     * @param name
     * @return
     */
    Object removeMetadata(String name);

    /**
     * Removes all the items in the metadata map.
     */
    void clearMetadataMap();

}
