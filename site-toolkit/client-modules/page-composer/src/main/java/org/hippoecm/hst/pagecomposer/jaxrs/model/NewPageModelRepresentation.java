/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.ArrayList;
import java.util.List;

public class NewPageModelRepresentation {

    private List<PrototypeRepresentation> prototypes;
    private List<Location> locations;


    public NewPageModelRepresentation() {}

    public NewPageModelRepresentation(final List<PrototypeRepresentation> prototypes,
                                      final List<SiteMapPageRepresentation> pages,
                                      final String hostName) {
        this.prototypes = prototypes;
        locations = new ArrayList<>();
        locations.add(new Location(hostName + "/", null));
        for (SiteMapPageRepresentation page : pages) {
            if (!page.isWorkspaceConfiguration()) {
                continue;
            }
            Location location = new Location(hostName + page.getRenderPathInfo() + "/", page.getId());
            locations.add(location);
        }

    }

    public List<PrototypeRepresentation> getPrototypes() {
        return prototypes;
    }

    public void setPrototypes(final List<PrototypeRepresentation> prototypes) {
        this.prototypes = prototypes;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(final List<Location> locations) {
        this.locations = locations;
    }

    public class Location {
        private String location;
        private String id;

        public Location(final String location, final String id) {
            this.location = location;
            this.id = id;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(final String location) {
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }
    }
}
