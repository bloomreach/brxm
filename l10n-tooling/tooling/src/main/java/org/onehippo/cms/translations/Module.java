/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.translations;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class Module {
    
    private final File pom;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private Registry registry;
    
    public Module(File pom) throws IOException {
        this.pom = pom;
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            final Model model = reader.read(new FileReader(pom));
            groupId = model.getGroupId();
            artifactId = model.getArtifactId();
            version = model.getVersion();
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }
    
    public String getId() {
        return groupId + ":" + artifactId + ":" + version;
    }
    
    public Registry getRegistry() {
        if (registry == null && hasResources()) {
            registry = new Registry(getResources()); 
        }
        return registry;
    }
    
    public boolean hasResources() {
        return getResources().exists();
    }
    
    private File getResources() {
        return new File(pom.getParent(), "resources");
    }


}
