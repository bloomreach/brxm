/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
        @FieldGroup(value = {
                "address", "latitude", "longitude"
        }, titleKey = "location"),
        @FieldGroup(value = {
                "mapType", "zoomFactor", "width", "height", "apiKey"
        }, titleKey = "map.options")
})

public interface EssentialsGoogleMapsComponentInfo {

    @Parameter(name = "latitude", required = false, displayName = "Latitude")
    double getLatitude();

    @Parameter(name = "longitude", required = false, displayName = "Longitude")
    double getLongitude();

    @DropDownList(value = {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21"})
    @Parameter(name = "zoomFactor", required = true, defaultValue = "8", displayName = "Zoom factor")
    String getZoomFactor();

    @Parameter(name = "mapType", required = false, displayName = "Map type",defaultValue = "ROADMAP")
    @DropDownList(value = {"ROADMAP","SATELLITE","TERRAIN","HYBRID"})
    String getMapType();

    @Parameter(name = "width", required = true, defaultValue = "400", displayName = "Width (in pixels)")
    int getWidth();

    @Parameter(name = "height", required = true, defaultValue = "400", displayName = "Height (in pixels)")
    int getHeight();

    @Parameter(name = "address", required = false, defaultValue = "", displayName = "Location / Address")
    String getAddress();

    @Parameter(name = "apiKey", required = false, displayName = "Google Maps API Key")
    String getApiKey();

}
