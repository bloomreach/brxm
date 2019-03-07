/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.hosting;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;

public class AutoHostTemplateTest {
    private static final String SAMPLE_URL_WITH_ASTERIX_REGEX = "*-eng.example.org";

    private static final List<String> validURLPatterns = Arrays.asList(
        "http://*-eng.org",
        "http://eng-*.org",
        "http://eng-*-hippo.org",
        "http://*.org",
        "http://example.org",
        "http://hippo.example.org",
        "http://test.hippo.example.org",
        "http://*.org",
        "http://*-eng.org:9090",
        "http://example.org:9090"
    );

    private static final List<String> invalidURLPatterns = Arrays.asList(
        "http://*",
        "http://**.org",
        "http://hippo.*.org",
        "http://*-eng.*-eng.org",
        "http://org",
        "http://*-eng",
        "http://eng-*",
        "http://hippo.*",
        "http://*-eng.org:aaaa",
        "http://*-eng.org:9",
        "http://*-eng.org:90bb",
        "http://*-eng.org:9090900",
        "http://*-eng.org:",
        "http://*-eng.org:9090:",
        "http://*-eng.org:9090:9090"
    );

    private static final List<String> validAsterixURLs = Arrays.asList(
        "-eng.example.org",
        "cms-eng.example.org",
        "site-eng.example.org"
    );

    private static final List<String> invalidAsterixURLs = Arrays.asList(
        "www.example.org",
        "cms.example.org",
        "site.example.org",
        "eng-site.cms.org"
    );

    @Test
    public void validateAutoHostTemplateURLScheme() {
        Pattern pattern = Pattern.compile(VirtualHostsService.AUTOHOST_URL_SCHEME_REGEX);
        assertTrue(pattern.matcher("https://").matches());
        assertTrue(pattern.matcher("http://").matches());
        assertFalse(pattern.matcher("htps://").matches());
        assertFalse(pattern.matcher("http:/").matches());
        assertFalse(pattern.matcher("http//").matches());
    }

    @Test
    public void validateAutoHostTemplateURLAsterix() {
        Pattern pattern = Pattern.compile(VirtualHostsService.AUTOHOST_URL_ASTERIX_REGEX);
        assertTrue(pattern.matcher("").matches());
        assertTrue(pattern.matcher("*").matches());
        assertTrue(pattern.matcher("*-eng").matches());
        assertTrue(pattern.matcher("eng-*").matches());
        assertTrue(pattern.matcher("eng").matches());
        assertFalse(pattern.matcher("**").matches());
    }

    @Test
    public void validateAutoHostTemplateURLHost() {
        Pattern pattern = Pattern.compile(VirtualHostsService.AUTOHOST_URL_HOST_REGEX);
        assertTrue(pattern.matcher(".example.com").matches());
        assertTrue(pattern.matcher(".example.test.com").matches());
        assertFalse(pattern.matcher("example").matches());
        assertFalse(pattern.matcher("").matches());
        assertFalse(pattern.matcher("*.example.com").matches());
        assertFalse(pattern.matcher("example.*.com").matches());
        assertFalse(pattern.matcher("example$").matches());
        assertFalse(pattern.matcher("example:").matches());
        assertFalse(pattern.matcher(".example.*aaaa").matches());
    }

    @Test
    public void validateAutoHostTemplateURLPort() {
        Pattern pattern = Pattern.compile(VirtualHostsService.AUTOHOST_URL_PORT_NUMBER_REGEX);
        assertTrue(pattern.matcher(":90").matches());
        assertTrue(pattern.matcher(":90909").matches());
        assertFalse(pattern.matcher(":9").matches());
        assertFalse(pattern.matcher(":aaaa").matches());
        assertFalse(pattern.matcher(":9aa").matches());
        assertFalse(pattern.matcher(":909:").matches());
        assertFalse(pattern.matcher(":909090").matches());
        assertFalse(pattern.matcher(":9090:9090").matches());
    }

    @Test
    public void validateAutoHostTemplateURLPatterns() {
        Pattern pattern = Pattern.compile(VirtualHostsService.AUTO_HOST_TEMPLATE_URL_REGEX);

        validURLPatterns.stream().forEach(url -> {
            assertTrue("Validation failed for url " + url, pattern.matcher(url).matches());
        });

        invalidURLPatterns.stream().forEach(url -> {
            assertFalse("Validation passed for url " + url, pattern.matcher(url).matches());
        });
    }

    @Test
    public void validateAutoHostTemplateURLAsterixPatterns() {
        Pattern pattern = Pattern.compile(
                SAMPLE_URL_WITH_ASTERIX_REGEX.replace("*", VirtualHostsService.AUTO_HOST_TEMPLATE_URL_ASTERIX_REGEX));

        validAsterixURLs.stream().forEach(url -> {
            assertTrue("Validation failed for url " + url, pattern.matcher(url).matches());
        });

        invalidAsterixURLs.stream().forEach(url -> {
            assertFalse("Validation passed for url " + url, pattern.matcher(url).matches());
        });
    }
}
