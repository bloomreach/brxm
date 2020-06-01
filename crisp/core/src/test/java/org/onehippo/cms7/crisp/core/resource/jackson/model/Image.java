/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource.jackson.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Image {

    private String source;
    private String name;
    private int hOffset;
    private int vOffset;
    private String alignment;
    private int[] exifVersion;
    private String[] exifGeoLocation;

    @JsonProperty("src")
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int gethOffset() {
        return hOffset;
    }

    public void sethOffset(int hOffset) {
        this.hOffset = hOffset;
    }

    public int getvOffset() {
        return vOffset;
    }

    public void setvOffset(int vOffset) {
        this.vOffset = vOffset;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public int[] getExifVersion() {
        return exifVersion;
    }

    public void setExifVersion(int[] exifVersion) {
        this.exifVersion = exifVersion;
    }

    public String[] getExifGeoLocation() {
        return exifGeoLocation;
    }

    public void setExifGeoLocation(String[] exifGeoLocation) {
        this.exifGeoLocation = exifGeoLocation;
    }

}
