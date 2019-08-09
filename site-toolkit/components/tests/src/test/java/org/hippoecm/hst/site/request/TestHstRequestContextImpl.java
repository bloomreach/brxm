/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.request;

import java.util.Map;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConfigurationImpl;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import com.google.common.collect.Iterables;

import static org.hippoecm.hst.site.request.HstRequestContextImpl.IS_SEARCH_ENGINE_OR_BOT_REQUEST_ATTR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TestHstRequestContextImpl
 * @version $Id$
 */
public class TestHstRequestContextImpl {

    private static Logger log = LoggerFactory.getLogger(TestHstRequestContextImpl.class);

    private static final String PROPS_RES_PATH = "/" + SpringComponentManager.class.getName().replace(".", "/")
            + ".properties";

    private static final String[] COMMON_BROWSER_USER_AGENT_EXAMPLES = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36", // chrome
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:53.0) Gecko/20100101 Firefox/53.0", // firefox
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393", // edge
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)", // ie6
            "Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US)", // ie7
            "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)", // ie8
            "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0;  Trident/5.0)", // ie9
            "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0; MDDCJS)", // ie10
            "Mozilla/5.0 (compatible, MSIE 11, Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko", // ie11
            "Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12H321 Safari/600.1.4", // ipad
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1", // iphone
            "Mozilla/5.0 (Linux; Android 6.0.1; SAMSUNG SM-G570Y Build/MMB29K) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/4.0 Chrome/44.0.2403.133 Mobile Safari/537.36", // samsung phone
            "Mozilla/5.0 (Linux; Android 5.0; SAMSUNG SM-N900 Build/LRX21V) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/2.1 Chrome/34.0.1847.76 Mobile Safari/537.36", // samsung galaxy note 3
            "Mozilla/5.0 (Linux; Android 6.0.1; SAMSUNG SM-N910F Build/MMB29M) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/4.0 Chrome/44.0.2403.133 Mobile Safari/537.36", // samsung galaxy note 3
            "Mozilla/5.0 (Linux; U; Android-4.0.3; en-us; Galaxy Nexus Build/IML74K) AppleWebKit/535.7 (KHTML, like Gecko) CrMo/16.0.912.75 Mobile Safari/535.7", // google nexus
            "Mozilla/5.0 (Linux; Android 7.0; HTC 10 Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36", // htc
            "curl/7.35.0", // curl
            "Wget/1.15 (linux-gnu)", // wget
            "Lynx/2.8.8pre.4 libwww-FM/2.14 SSL-MM/1.4.1 GNUTLS/2.12.23", // lynx
    };

    private static final String[] COMMON_SEARCH_ENGINE_USER_AGENT_EXAMPLES = {
            // AOL
            "Mozilla/5.0 (compatible; MSIE 9.0; AOL 9.7; AOLBuild 4343.19; Windows NT 6.1; WOW64; Trident/5.0; FunWebProducts)",
            "Mozilla/4.0 (compatible; MSIE 8.0; AOL 9.7; AOLBuild 4343.27; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)",
            "Mozilla/4.0 (compatible; MSIE 8.0; AOL 9.7; AOLBuild 4343.21; Windows NT 5.1; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.04506.648; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET4.0C; .NET4.0E)",
            "Mozilla/4.0 (compatible; MSIE 8.0; AOL 9.7; AOLBuild 4343.19; Windows NT 5.1; Trident/4.0; GTB7.2; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)",
            "Mozilla/4.0 (compatible; MSIE 8.0; AOL 9.7; AOLBuild 4343.19; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.04506.648; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET4.0C; .NET4.0E)",
            "Mozilla/4.0 (compatible; MSIE 7.0; AOL 9.7; AOLBuild 4343.19; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.04506.648; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET4.0C; .NET4.0E)",
            // Baidu
            "Baiduspider",
            "Baidu Web Search",
            "Baidu Image Search",
            "Baiduspider-image",
            "Baidu Mobile Search",
            "Baiduspider-mobile",
            "Baidu Video Search",
            "Baiduspider-video",
            "Baidu News Search",
            "Baiduspider-news",
            "Baidu Bookmark Search",
            "Baiduspider-favo",
            "Baidu Union Search",
            "Baiduspider-cpro",
            "Baidu Business Search",
            "Baiduspider-ads",
            "Mozilla/5.0 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)",
            "Baiduspider+(+http://www.baidu.com/search/spider_jp.html)",
            "Baiduspider+(+http://www.baidu.com/search/spider.htm)",
            // Bingbot / MSN
            "Mozilla/5.0 (compatible; bingbot/2.0 +http://www.bing.com/bingbot.htm)",
            "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)",
            "Mozilla/5.0 (Windows Phone 8.1; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 530) like Gecko (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)",
            "msnbot/2.0b (+http://search.msn.com/msnbot.htm)",
            "msnbot-media/1.1 (+http://search.msn.com/msnbot.htm)",
            "Mozilla/5.0 (compatible; adidxbot/2.0; +http://www.bing.com/bingbot.htm)",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53 (compatible; adidxbot/2.0; +http://www.bing.com/bingbot.htm)",
            "Mozilla/5.0 (Windows Phone 8.1; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 530) like Gecko (compatible; adidxbot/2.0; +http://www.bing.com/bingbot.htm)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534+ (KHTML, like Gecko) BingPreview/1.0b",
            "Mozilla/5.0 (Windows Phone 8.1; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 530) like Gecko BingPreview/1.0b",
            // DuckDuckGo
            "DuckDuckBot/1.0; (+http://duckduckgo.com/duckduckbot.html)",
            // Google
            "Googlebot/2.1 (+http://www.googlebot.com/bot.html)",
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "Googlebot/2.1 (+http://www.google.com/bot.html)",
            "Googlebot-News",
            "Googlebot-Image/1.0",
            "Googlebot-Video/1.0",
            "SAMSUNG-SGH-E250/1.0 Profile/MIDP-2.0 Configuration/CLDC-1.1 UP.Browser/6.2.3.3.c.1.101 (GUI) MMP/2.0 (compatible; Googlebot-Mobile/2.1; +http://www.google.com/bot.html)",
            "DoCoMo/2.0 N905i(c100;TB;W24H16) (compatible; Googlebot-Mobile/2.1; +http://www.google.com/bot.html)",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 8_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12F70 Safari/600.1.4 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "[various mobile device types] (compatible; Mediapartners-Google/2.1; +http://www.google.com/bot.html)",
            "Mediapartners-Google",
            "AdsBot-Google (+http://www.google.com/adsbot.html)",
            // Teoma
            "Mozilla/2.0 (compatible; Ask Jeeves/Teoma; +http://sp.ask.com/docs/about/tech_crawling.html)",
            "Mozilla/2.0 (compatible; Ask Jeeves/Teoma; +http://about.ask.com/en/docs/about/webmasters.shtml)",
            "Mozilla/2.0 (compatible; Ask Jeeves/Teoma)",
            "Mozilla/5.0 (compatible; Ask Jeeves/Teoma; +http://about.ask.com/en/docs/about/webmasters.shtml)",
            // Yahoo
            "Mozilla/5.0 (compatible; Yahoo! Slurp; http://help.yahoo.com/help/us/ysearch/slurp)",
            // Yandex
            "Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 8_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B411 Safari/600.1.4 (compatible; YandexBot/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexAccessibilityBot/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 8_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B411 Safari/600.1.4 (compatible; YandexMobileBot/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexDirectDyn/1.0; +http://yandex.com/bots",
            "Mozilla/5.0 (compatible; YandexImages/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexVideo/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexMedia/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexBlogs/0.99; robot; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexFavicons/1.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexWebmaster/2.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexPagechecker/1.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexImageResizer/2.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YaDirectFetcher/1.0; Dyatel; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexCalendar/1.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexSitelinks; Dyatel; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexMetrika/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexAntivirus/2.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexVertis/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; YandexBot/3.0; MirrorDetector; +http://yandex.com/",
    };

    private ContainerConfiguration containerConfiguration;

    @Before
    public void setUp() throws Exception {
        containerConfiguration = new ContainerConfigurationImpl(
                new PropertiesConfiguration(TestHstRequestContextImpl.class.getResource(PROPS_RES_PATH)));

        final String[] defaultSeoUserAgentPatterns = containerConfiguration
                .getStringArray(ContainerConstants.DEFAULT_SEARCH_ENGINE_OR_BOT_USER_AGENT_PATTERNS);
        assertNotNull(defaultSeoUserAgentPatterns);
        assertTrue(defaultSeoUserAgentPatterns.length > 0);

        // check the default patterns property with comma separated string resulting in non-empty, trimmed string elements array.
        for (String pattern : defaultSeoUserAgentPatterns) {
            assertNotNull(pattern);
            assertFalse(pattern.isEmpty());
            assertEquals(pattern.trim(), pattern);
        }

        final String[] extraSeoUserAgentPatterns = containerConfiguration
                .getStringArray(ContainerConstants.EXTRA_SEARCH_ENGINE_OR_BOT_USER_AGENT_PATTERNS);
        assertNotNull(extraSeoUserAgentPatterns);
        // a property line with a blank after equal sign results in a String[] { "" }.
        assertEquals(1, extraSeoUserAgentPatterns.length);
        assertEquals("", extraSeoUserAgentPatterns[0]);
    }

    @Test
    public void testModelAndAttributes() {
        HstRequestContextImpl requestContext = new HstRequestContextImpl(null);
        assertTrue(requestContext.attributes == null || requestContext.attributes.isEmpty());

        final Map<String, Object> attributes = requestContext.getAttributes();
        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());

        try {
            attributes.put("attr1", "value1");
            fail("Must not allow to add attribute directly from the attributes map.");
        } catch (UnsupportedOperationException expected) {
        }

        requestContext.setModel("model1", "modelValue1");
        assertEquals("modelValue1", requestContext.getAttribute("model1"));

        requestContext.setAttribute("attr1", "value1");
        assertEquals("value1", requestContext.getAttribute("attr1"));
        assertEquals("modelValue1", requestContext.getAttribute("model1"));
        assertNull(requestContext.getModel("attr1"));

        assertEquals("value1", requestContext.getAttributes().get("attr1"));
        assertEquals("modelValue1", requestContext.getAttributes().get("model1"));

        assertTrue(Iterables.contains(requestContext.getModelNames(), "model1"));
        assertFalse(Iterables.contains(requestContext.getModelNames(), "attr1"));
        assertTrue(EnumerationUtils.toList(requestContext.getAttributeNames()).contains("attr1"));
        assertTrue(EnumerationUtils.toList(requestContext.getAttributeNames()).contains("model1"));

        assertEquals("value1", requestContext.getAttributes().get("attr1"));
        assertFalse(requestContext.attributes.isEmpty());
        assertFalse(requestContext.getAttributes().isEmpty());

        requestContext.removeModel("model1");
        assertNull(requestContext.getModel("model1"));
        assertNull(requestContext.getModelsMap().get("model1"));
        assertTrue(requestContext.getModelsMap().isEmpty());

        requestContext.removeAttribute("attr1");
        requestContext.removeAttribute("model1");
        assertNull(requestContext.getAttribute("attr1"));
        assertNull(requestContext.getAttributes().get("attr1"));
        assertTrue(requestContext.attributes.isEmpty());
        assertTrue(requestContext.getAttributes().isEmpty());
    }

    @Test
    public void testIsSearchEngineRequestWithBrowserUserAgents() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        HstRequestContextImpl requestContext = new HstRequestContextImpl(null);
        requestContext.setContainerConfiguration(containerConfiguration);
        requestContext.setServletRequest(servletRequest);

        servletRequest.removeHeader("User-Agent");
        assertFalse(requestContext.isSearchEngineOrBotRequest());
        requestContext.removeAttribute(IS_SEARCH_ENGINE_OR_BOT_REQUEST_ATTR);
        servletRequest.addHeader("User-Agent", "");
        assertFalse(requestContext.isSearchEngineOrBotRequest());
        requestContext.removeAttribute(IS_SEARCH_ENGINE_OR_BOT_REQUEST_ATTR);

        for (String userAgent : COMMON_BROWSER_USER_AGENT_EXAMPLES) {
            servletRequest.removeHeader("User-Agent");
            servletRequest.addHeader("User-Agent", userAgent);
            assertFalse(String.format("Expected User-Agent '%s' to be a browser user agent, not a search engine or bot",
                    userAgent), requestContext.isSearchEngineOrBotRequest());
            requestContext.removeAttribute(IS_SEARCH_ENGINE_OR_BOT_REQUEST_ATTR);
        }
        assertEquals(COMMON_BROWSER_USER_AGENT_EXAMPLES.length, requestContext.userAgentsBots.size());
    }

    @Test
    public void testIsSearchEngineRequestWithSearchEngineUserAgents() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        HstRequestContextImpl requestContext = new HstRequestContextImpl(null);
        requestContext.setContainerConfiguration(containerConfiguration);
        requestContext.setServletRequest(servletRequest);

        for (String userAgent : COMMON_SEARCH_ENGINE_USER_AGENT_EXAMPLES) {
            servletRequest.removeHeader("User-Agent");
            servletRequest.addHeader("User-Agent", userAgent);
            assertTrue(requestContext.isSearchEngineOrBotRequest());
            requestContext.removeAttribute(IS_SEARCH_ENGINE_OR_BOT_REQUEST_ATTR);
        }

        assertEquals(COMMON_SEARCH_ENGINE_USER_AGENT_EXAMPLES.length, requestContext.userAgentsBots.size());
    }

}
