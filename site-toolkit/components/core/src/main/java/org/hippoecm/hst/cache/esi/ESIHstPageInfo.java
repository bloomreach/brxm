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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.constructs.web.Header;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.james.mime4j.util.MimeUtil;
import org.hippoecm.hst.cache.HstPageInfo;

/**
 * ESIHstPageInfo
 */
public class ESIHstPageInfo extends HstPageInfo {

    private static final long serialVersionUID = 1L;

    private String ungzippedBodyAsString;
    private List<ESIFragmentInfo> fragmentInfos;

    public ESIHstPageInfo(int statusCode, String contentType, Collection cookies, byte [] body, String characterEncoding,
            long timeToLiveSeconds, Collection<Header<? extends Serializable>> headers) throws UnsupportedEncodingException {
        this(statusCode, contentType, cookies, bytesToString(body, characterEncoding, contentType), characterEncoding, timeToLiveSeconds, headers);
    }

    public ESIHstPageInfo(int statusCode, String contentType, Collection cookies, String ungzippedBodyAsString, String characterEncoding,
            long timeToLiveSeconds, Collection<Header<? extends Serializable>> headers) {
        super(statusCode, contentType, cookies, ArrayUtils.EMPTY_BYTE_ARRAY, characterEncoding, timeToLiveSeconds, headers);
        this.ungzippedBodyAsString = ungzippedBodyAsString;
    }

    public String getUngzippedBodyAsString() {
        return ungzippedBodyAsString;
    }

    public void addAllFragmentInfos(Collection<ESIFragmentInfo> fragmentInfos) {
        if (this.fragmentInfos == null) {
            this.fragmentInfos = new LinkedList<ESIFragmentInfo>();
        }

        this.fragmentInfos.addAll(fragmentInfos);
    }

    public void addFragmentInfo(ESIFragmentInfo fragmentInfo) {
        if (fragmentInfos == null) {
            fragmentInfos = new LinkedList<ESIFragmentInfo>();
        }

        fragmentInfos.add(fragmentInfo);
    }

    public void removeAllFragmentInfos() {
        if (fragmentInfos != null) {
            fragmentInfos.clear();
        }
    }

    public List<ESIFragmentInfo> getFragmentInfos() {
        if (fragmentInfos != null) {
            return Collections.unmodifiableList(fragmentInfos);
        }

        return Collections.emptyList();
    }

    public boolean hasAnyFragmentInfo() {
        return (fragmentInfos != null && !fragmentInfos.isEmpty());
    }

    private static String bytesToString(byte [] body, String characterEncoding, String contentType) throws UnsupportedEncodingException {
        if (body == null || body.length == 0) {
            return "";
        }

        if (characterEncoding == null && contentType != null) {
            Map<String, String> params = MimeUtil.getHeaderParams(contentType);
            String charset = params.get("charset");

            if (StringUtils.isNotBlank(charset)) {
                characterEncoding = charset;
            }
        }

        return new String(body, characterEncoding);
    }

}
