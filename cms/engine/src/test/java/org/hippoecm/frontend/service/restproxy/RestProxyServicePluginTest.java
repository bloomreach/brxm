/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service.restproxy;


import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;

public class RestProxyServicePluginTest {

    @Test
    public void valid_uri_with_port_number() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String restURI = RestProxyServicePlugin.createRestURI("http://127.0.0.1:8080/site/_cmsrest", request);
        assertEquals("http://127.0.0.1:8080/site/_cmsrest", restURI);
    }

    @Test
    public void valid_uri_without_port_number_takes_port_from_request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalPort(8081);
        String restURI = RestProxyServicePlugin.createRestURI("http://127.0.0.1/site/_cmsrest", request);
        assertEquals("http://127.0.0.1:8081/site/_cmsrest", restURI);
    }

    @Test
    public void valid_uri_without_port_number_and_request_on_80() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalPort(80);
        String restURI = RestProxyServicePlugin.createRestURI("http://127.0.0.1/site/_cmsrest", request);
        assertEquals("http://127.0.0.1:80/site/_cmsrest", restURI);
    }

    @Test
    public void valid_uri_without_port_number_and_request_on_443() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalPort(443);
        String restURI = RestProxyServicePlugin.createRestURI("http://127.0.0.1/site/_cmsrest", request);
        // odd situation because rest uri uses http
        assertEquals("http://127.0.0.1:443/site/_cmsrest", restURI);
    }

    @Test
    public void valid_ipv6_uri() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalPort(8081);
        String restURI = RestProxyServicePlugin.createRestURI("http://[0:0:0:0:0:0:0:1]/site/_cmsrest", request);
        assertEquals("http://[0:0:0:0:0:0:0:1]:8081/site/_cmsrest", restURI);
    }
    @Test
    public void valid_ipv6_uri_reduced() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalPort(8081);
        String restURI = RestProxyServicePlugin.createRestURI("http://[::1]/site/_cmsrest", request);
        assertEquals("http://[::1]:8081/site/_cmsrest", restURI);
    }

    @Test
    public void valid_ipv6_uri_with_port() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String restURI = RestProxyServicePlugin.createRestURI("http://[0:0:0:0:0:0:0:1]:8080/site/_cmsrest", request);
        assertEquals("http://[0:0:0:0:0:0:0:1]:8080/site/_cmsrest", restURI);
    }
    @Test
    public void valid_ipv6_uri_reduced_with_port() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String restURI = RestProxyServicePlugin.createRestURI("http://[::1]:8080/site/_cmsrest", request);
        assertEquals("http://[::1]:8080/site/_cmsrest", restURI);
    }

    @Test(expected = IllegalStateException.class)
    public void invalid_uri_1() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String restURI = RestProxyServicePlugin.createRestURI(null, request);
        System.out.println(restURI);
    }

    @Test(expected = IllegalStateException.class)
    public void invalid_uri_2() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String restURI = RestProxyServicePlugin.createRestURI("", request);
        System.out.println(restURI);
    }

    @Test(expected = IllegalStateException.class)
    public void invalid_uri_3() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String restURI = RestProxyServicePlugin.createRestURI("http:wdfqwefwwe/qwe", request);
        System.out.println(restURI);
    }

}
