/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.servlet;

import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyConfigReaderTest {
    @Test
    public void getProxies_proxiesAsStringEmpty_returnEmptyCollection() throws Exception {
        assertThat(ProxyConfigReader.getProxies("", "")).isEmpty();
    }

    @Test
    public void getProxies_proxiesAsStringTwoProxies_returnTwoProxyConfigs(){
        String proxiesAsString = "angular/hippo-cm@http://127.0.0.1:9090,angular/hippo-projects@http://127.0.0.1:9092";
        final Set<ProxyConfig> angular = ProxyConfigReader.getProxies("/angular", proxiesAsString);
        assertThat(angular).contains(new ProxyConfig("/hippo-cm/","http://127.0.0.1:9090/"))
                .contains(new ProxyConfig("/hippo-projects/","http://127.0.0.1:9092/"));
    }

    @Test
    public void getProxies_proxiesAsStringTwoProxies_space_after_comma_returnTwoProxyConfigs(){
        String proxiesAsString = "angular/hippo-cm@http://127.0.0.1:9090, angular/hippo-projects@http://127.0.0.1:9092";
        final Set<ProxyConfig> angular = ProxyConfigReader.getProxies("/angular", proxiesAsString);
        assertThat(angular).contains(new ProxyConfig("/hippo-cm/","http://127.0.0.1:9090/"))
                .contains(new ProxyConfig("/hippo-projects/","http://127.0.0.1:9092/"));
    }

}
