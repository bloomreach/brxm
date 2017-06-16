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
package org.onehippo.cm.migration;

import org.junit.Ignore;
import org.junit.Test;

public class Esv2YamlTest {

    @Test
    @Ignore
    public void main() throws Exception {
//        FileConfigurationWriter writer = new MigrationConfigWriter(MigrationConfigWriter.MoveType.GIT);
//        String target = "/Users/sshepelevich/Temp/conversion";
//        String src = "/Users/sshepelevich/code/hippo/cms-community/hippo-cms/master/editor/repository/src/test/resources";
//        String target = "/Users/sshepelevich/code/hippo/cms-community/hippo-cms/master/editor/repository/src/test/resources";
//        Esv2Yaml.main(new String[]{"-s", src, "-t", target, "-m", "COPY"});

//        {
//            String src = "/Users/sshepelevich/Documents/Projects/myhippoproject/bootstrap/configuration/src/main/resources";
//            String target = "/Users/sshepelevich/Documents/Projects/myhippoproject/bootstrap/configuration/src/main/resources";

//        String src = "/Users/sshepelevich/code/hippo/cms-community/hippo-testsuite/master/content/src/main/resources";
        String target = "/Users/sshepelevich/code/hippo/engineering/hippo-gogreen-enterprise/master/enterprise/server-config/test-server-config/src/main/resources";
            Esv2Yaml.main(new String[]{"-s", target, "-t", target, "-m", "git"});
//        }

//        {
//            String src = "/Users/sshepelevich/Documents/Projects/myhippoproject/bootstrap/content/src/main/resources";
//            String target = "/Users/sshepelevich/Documents/Projects/myhippoproject/bootstrap/content/src/main/resources";
//            Esv2Yaml.main(new String[]{"-s", src, "-t", target, "-m", "GIT"});
//        }
    }

}