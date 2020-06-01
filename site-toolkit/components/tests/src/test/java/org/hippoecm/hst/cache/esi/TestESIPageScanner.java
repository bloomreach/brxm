/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.esi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TestESIPageScanner
 */
public class TestESIPageScanner {

    private static Logger log = LoggerFactory.getLogger(TestESIPageScanner.class);

    @Test
    public void testESIFragmentsScanning() throws Exception {
        String bodyContent = new String(readURLContentAsByteArray(getClass().getResource("esi-source-page-1.html")), "UTF-8");
        ESIPageScanner scanner = new ESIPageScanner();
        List<ESIFragmentInfo> fragmentInfos = scanner.scanFragmentInfos(bodyContent);

        assertEquals(7, fragmentInfos.size());

        ESIFragmentInfo fragmentInfo = fragmentInfos.get(0);
        assertEquals(ESIFragmentType.COMMENT_BLOCK, fragmentInfo.getFragment().getType());
        assertTrue(fragmentInfo instanceof ESICommentFragmentInfo);
        assertTrue(fragmentInfo.getFragment() instanceof ESICommentFragment);
        assertEquals("<h1>Tags in ESI Comment</h1>", StringUtils.trim(fragmentInfo.getFragment().getSource()));
        assertFalse(((ESICommentFragmentInfo) fragmentInfo).hasAnyFragmentInfo());

        fragmentInfo = fragmentInfos.get(1);
        assertEquals(ESIFragmentType.COMMENT_TAG, fragmentInfo.getFragment().getType());
        assertTrue(fragmentInfo instanceof ESIElementFragmentInfo);
        assertTrue(fragmentInfo.getFragment() instanceof ESIElementFragment);
        assertTrue(StringUtils.contains(fragmentInfo.getFragment().getSource(), "Show the license info."));

        fragmentInfo = fragmentInfos.get(2);
        assertEquals(ESIFragmentType.INCLUDE_TAG, fragmentInfo.getFragment().getType());
        assertTrue(fragmentInfo instanceof ESIElementFragmentInfo);
        assertTrue(fragmentInfo.getFragment() instanceof ESIElementFragment);
        assertEquals("http://www.example.com/LICENSE", ((ESIElementFragment) fragmentInfo.getFragment()).getElement().getAttribute("src"));

        fragmentInfo = fragmentInfos.get(3);
        assertEquals(ESIFragmentType.REMOVE_TAG, fragmentInfo.getFragment().getType());
        assertTrue(fragmentInfo instanceof ESIElementFragmentInfo);
        assertTrue(fragmentInfo.getFragment() instanceof ESIElementFragment);
        assertTrue(StringUtils.contains(fragmentInfo.getFragment().getSource(), "<h1>Test Remove 1</h1>"));

        fragmentInfo = fragmentInfos.get(4);
        assertEquals(ESIFragmentType.REMOVE_TAG, fragmentInfo.getFragment().getType());
        assertTrue(fragmentInfo instanceof ESIElementFragmentInfo);
        assertTrue(fragmentInfo.getFragment() instanceof ESIElementFragment);
        assertTrue(StringUtils.contains(fragmentInfo.getFragment().getSource(), "<a href='http://www.example.com/LICENSE'>The license</a>"));

        fragmentInfo = fragmentInfos.get(5);
        assertEquals(ESIFragmentType.COMMENT_BLOCK, fragmentInfo.getFragment().getType());
        assertTrue(fragmentInfo instanceof ESICommentFragmentInfo);
        assertTrue(fragmentInfo.getFragment() instanceof ESICommentFragment);
        assertTrue(StringUtils.contains(fragmentInfo.getFragment().getSource(), "<p>The full text of the license:</p>"));
        assertTrue(StringUtils.contains(fragmentInfo.getFragment().getSource(), "<div><esi:include src='http://example.com/LICENSE' /></div>"));
        assertTrue(((ESICommentFragmentInfo) fragmentInfo).hasAnyFragmentInfo());
        assertEquals(1, ((ESICommentFragmentInfo) fragmentInfo).getFragmentInfos().size());

        fragmentInfo = ((ESICommentFragmentInfo) fragmentInfo).getFragmentInfos().get(0);
        assertEquals(ESIFragmentType.INCLUDE_TAG, fragmentInfo.getFragment().getType());
        assertTrue(fragmentInfo instanceof ESIElementFragmentInfo);
        assertTrue(fragmentInfo.getFragment() instanceof ESIElementFragment);
        assertEquals("http://example.com/LICENSE", ((ESIElementFragment) fragmentInfo.getFragment()).getElement().getAttribute("src"));

        fragmentInfo = fragmentInfos.get(6);
        assertEquals(ESIFragmentType.VARS_TAG, fragmentInfo.getFragment().getType());
        assertTrue(fragmentInfo instanceof ESIElementFragmentInfo);
        assertTrue(fragmentInfo.getFragment() instanceof ESIElementFragment);
        assertTrue(StringUtils.contains(fragmentInfo.getFragment().getSource(), "<img src=\"http://www.example.com/$(HTTP_COOKIE{type})/hello.gif\"/>"));
        assertTrue(StringUtils.contains(fragmentInfo.getFragment().getSource(), "<li>User Agent: $(HTTP_USER_AGENT{browser}), $(HTTP_USER_AGENT{version}), $(HTTP_USER_AGENT{os})</li>"));
    }

    private byte [] readURLContentAsByteArray(URL url) throws IOException {
        InputStream is = null;

        try {
            is = url.openStream();
            return IOUtils.toByteArray(url.openStream());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
