/**
 * Copyright 2013-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cache;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.constructs.web.Header;
import net.sf.ehcache.constructs.web.PageInfo;

/**
 * Default HST implementation of PageInfo extending {@link net.sf.ehcache.constructs.web.PageInfo},
 * a Serializable representation of a {@link HttpServletResponse}.
 */
public class HstPageInfo extends PageInfo {

    private static final Logger log = LoggerFactory.getLogger(HstPageInfo.class);

    private static final long serialVersionUID = 1L;

    private String characterEncoding;
    private Boolean isNoCachePresentOrExpiresImmediately;

    public HstPageInfo() {
        super(HttpServletResponse.SC_OK, null, null, null, false, 0, null);
    }

    public HstPageInfo(final int statusCode, final String contentType, final Collection cookies, final byte[] body, String characterEncoding,
            long timeToLiveSeconds, final Collection<Header<? extends Serializable>> headers) {
        super(statusCode, contentType, cookies, body, false, timeToLiveSeconds, headers);
        this.characterEncoding = characterEncoding;
    }

    public String getCharacterEncoding() {
        if (characterEncoding == null) {
            String contentType = getContentType();

            if (contentType != null) {
                Map<String, String> params = MimeUtil.getHeaderParams(contentType);
                characterEncoding = params.get("charset");
            }
        }

        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    /**
     * Returns true 
     * @return
     */
    public boolean isNoCachePresentOrExpiresImmediately() {
        if (isNoCachePresentOrExpiresImmediately == null) {

            isNoCachePresentOrExpiresImmediately = Boolean.FALSE;

            final List<Header<? extends Serializable>> headers = getHeaders();
    
            for (Header<? extends Serializable> header : headers) {
                if ("Pragma".equalsIgnoreCase(header.getName()) && "no-cache".equals(header.getValue())) {
                    isNoCachePresentOrExpiresImmediately = Boolean.TRUE;
                } else if ("Cache-Control".equalsIgnoreCase(header.getName()) && StringUtils.contains(String.valueOf(header.getValue()), "no-cache")) {
                    isNoCachePresentOrExpiresImmediately = Boolean.TRUE;
                } else if ("Expires".equalsIgnoreCase(header.getName())) {
                    try {
                        final long time = getTime(header);
                        if (time <= System.currentTimeMillis()) {
                            isNoCachePresentOrExpiresImmediately = Boolean.TRUE;
                        }
                    } catch (ParseException e) {
                        log.warn("Could not parse 'Expires' header because has value '{}'.", header.getValue());
                    }
                }
            }

        }

        return isNoCachePresentOrExpiresImmediately.booleanValue();
    }

    /**
     * @return Returns expires in seconds until now if present and otherwise null
     */
    public Long getExpiresInSeconds() {
        final List<Header<? extends Serializable>> headers = getHeaders();
        for (Header<? extends Serializable> header : headers) {
            if ("Expires".equalsIgnoreCase(header.getName())) {
                try {
                    final long time = getTime(header);
                    return  (time - System.currentTimeMillis()) / 1000;
                } catch (ParseException e) {
                    log.warn("Could not parse 'Expires' header because has value '{}'.", header.getValue());
                    return null;
                }
            }
        }
        return null;
    }

    public void writeContent(final HttpServletResponse response) throws IOException
    {
        byte [] body = this.getUngzippedBody();
        response.setContentLength(body != null ? body.length : 0);
        OutputStream out = new BufferedOutputStream(response.getOutputStream());
        out.write(body);
        out.flush();
    }

    private long getTime(final Header expiresHeader) throws ParseException {
        try {
            return Long.parseLong(String.valueOf(expiresHeader.getValue()));
        } catch (NumberFormatException e) {
            // could not parse expires header to long, test whether the format is in RFC1123 format which is the
            // case when the date header is set via org.hippoecm.hst.core.component.HstResponseState.setDateHeader()
            final SimpleDateFormat rfc1123DateFormat = getRFC1123DateFormat();
            return rfc1123DateFormat.parse(String.valueOf(expiresHeader.getValue())).getTime();
        }
    }

    static SimpleDateFormat getRFC1123DateFormat() {
        // header must conform to RFC 1123 date format (please note that milliseconds are lost)
        final SimpleDateFormat rfc1123DateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        rfc1123DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return rfc1123DateFormat;
    }

}
