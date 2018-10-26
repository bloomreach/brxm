/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms7.essentials.components.providers.OpenStreetMapOverlaysProvider;
import org.onehippo.cms7.essentials.components.providers.OpenStreetMapTypesProvider;

@FieldGroupList({
    @FieldGroup(value = {"address", "latitude", "longitude", "showMarker", "markerCustomText"}, titleKey = "map.focus" ),
    @FieldGroup(value = {"mapType", "mapOverlay", "zoomFactor"}, titleKey = "map.canvas"),
    @FieldGroup(value = {"width", "height"}, titleKey = "map.size")
})

public interface EssentialsOpenStreetMapComponentInfo {
    @Parameter(name = "address")
    String getAddress();

    @Parameter(name = "latitude")
    double getLatitude();

    @Parameter(name = "longitude")
    double getLongitude();

    @Parameter(name = "markerCustomText")
    String getMarkerCustomText();
    
    @Parameter(name = "showMarker", defaultValue = "withoutText")
    @DropDownList(value = {"no", "withoutText", "withAddress", "withCustomText"})
    String getShowMarker();
    
    @Parameter(name = "mapType", required = true, defaultValue = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
    @DropDownList(valueListProvider = OpenStreetMapTypesProvider.class)
    String getMapType();

    @Parameter(name = "mapOverlay", defaultValue = "none")
    @DropDownList(valueListProvider = OpenStreetMapOverlaysProvider.class)
    String getMapOverlay();

    @DropDownList(value = {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18"})
    @Parameter(name = "zoomFactor", required = true, defaultValue = "16")
    String getZoomFactor();

    @Parameter(name = "width")
    int getWidth();

    @Parameter(name = "height")
    int getHeight();
}
