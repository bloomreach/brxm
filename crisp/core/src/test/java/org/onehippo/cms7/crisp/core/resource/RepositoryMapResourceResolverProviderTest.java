/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource;

import java.util.List;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.core.interceptor.SimpleOAuth2AuthorizedClientInterceptor;
import org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeFactory;
import org.onehippo.repository.mock.MockSession;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.test.util.AopTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RepositoryMapResourceResolverProviderTest {

    private RepositoryMapResourceResolverProvider resourceResolverProvider;
    private ClassPathXmlApplicationContext appCtx;

    @Before
    public void setUp() throws Exception {
        appCtx = new ClassPathXmlApplicationContext();
        appCtx.setConfigLocation(RepositoryMapResourceResolverProviderTest.class.getName().replace(".", "/") + ".xml");
        appCtx.refresh();

        resourceResolverProvider = new RepositoryMapResourceResolverProvider();
        resourceResolverProvider.setApplicationContext(appCtx);
    }

    private MockNode createMockNode() throws Exception {
        MockNode rootNode = MockNodeFactory.fromYaml(RepositoryMapResourceResolverProviderTest.class.getResource("SampleResourceResolver.yaml"));
        return rootNode;
    }

    @After
    public void tearDown() throws Exception {
        appCtx.stop();
        appCtx.close();
    }

    @Test
    public void testRefreshResourceResolvers() throws Exception {
        Session mockSession = new MockSession(createMockNode());
        Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        resourceResolverProvider.setCredentials(credentials);
        Repository repository = EasyMock.createNiceMock(Repository.class);
        resourceResolverProvider.setRepository(repository);
        EasyMock.expect(repository.login(credentials)).andStubReturn(mockSession);
        EasyMock.replay(repository);

        resourceResolverProvider.refreshResourceResolvers();
        Map<String, ResourceResolver> resourceResolverMap = resourceResolverProvider.getResourceResolverMap();
        ResourceResolver resolver = resourceResolverMap.get("salesforce");
        assertNotNull(resolver);
        resolver = AopTestUtils.getTargetObject(resolver);
        assertTrue(resolver instanceof SimpleJacksonRestTemplateResourceResolver);
        SimpleJacksonRestTemplateResourceResolver salesForceResolver = (SimpleJacksonRestTemplateResourceResolver) resolver;
        assertEquals("https://na1.salesforce.com/services/data/v20.0", salesForceResolver.getBaseUri());
        List<ClientHttpRequestInterceptor> interceptors = salesForceResolver.getClientHttpRequestInterceptor();
        assertEquals(1, interceptors.size());
        ClientHttpRequestInterceptor interceptor = interceptors.get(0);
        assertTrue(interceptor instanceof SimpleOAuth2AuthorizedClientInterceptor);
        SimpleOAuth2AuthorizedClientInterceptor oAuth2Interceptor = (SimpleOAuth2AuthorizedClientInterceptor) interceptor;
        assertEquals("john.doe@example.com", oAuth2Interceptor.getContextAttributesMap().get(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME));
        assertEquals("somePassword", oAuth2Interceptor.getContextAttributesMap().get(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME));
        ClientRegistration clientRegistration = oAuth2Interceptor.getClientRegistrationRepository().findByRegistrationId("salesforce-login");
        assertNotNull(clientRegistration);
        assertEquals("someClientId", clientRegistration.getClientId());
        assertEquals("someClientSecret", clientRegistration.getClientSecret());
        ClientRegistration.ProviderDetails providerDetails = clientRegistration.getProviderDetails();
        assertNotNull(providerDetails);
        assertEquals("https://na1.salesforce.com/services/oauth2/token", providerDetails.getAuthorizationUri());
        assertEquals("https://na1.salesforce.com/services/oauth2/token", providerDetails.getTokenUri());
    }
}
