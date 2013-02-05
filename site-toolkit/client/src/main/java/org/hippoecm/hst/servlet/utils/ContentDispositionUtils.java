/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.hippoecm.hst.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for setting content disposition headers on responses.
 */
public final class ContentDispositionUtils {

    private static final Logger log = LoggerFactory.getLogger(ContentDispositionUtils.class);

    /**
     * The default encoding for content disposition fileName is 'user-agent-agnostic', also see 
     * {@link #encodeContentDispositionFileName(HttpServletRequest, HttpServletResponse, String)}
     */

    public static final String USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING = "user-agent-agnostic";

    public static final String USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING = "user-agent-specific";

    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Hide constructor of utility class
     */
    private ContentDispositionUtils() {
    }

    /**
     * Test if a mime type matches any of the content disposition types.
     * <p/>
     * The content disposition types can have a '*' as wildcards.
     *  
     * @param mimeType the mime type to test
     * @param contentDispositionContentTypes content disposition types.
     * @return true is the mime type matches any of the content disposition types.
     */
    public static boolean isContentDispositionType(String mimeType, Set<String> contentDispositionContentTypes) {
        boolean isContentDispositionType = contentDispositionContentTypes.contains(mimeType);
        if (!isContentDispositionType) {
            isContentDispositionType = contentDispositionContentTypes.contains("*/*");
            if (!isContentDispositionType) {
                int offset = mimeType.indexOf('/');
                if (offset != -1) {
                    isContentDispositionType = contentDispositionContentTypes.contains(mimeType.substring(0, offset)
                            + "/*");
                }
            }
        }
        return isContentDispositionType;
    }

    /**
     * Adds a Content-Disposition header to the given <code>binaryFileNode</code>. The HTTP header is only set when the
     * content type matches one of the configured <code>contentDispositionContentTypes</code>.
     *
     * <p/>When the Content-Disposition header is set, the filename is retrieved from the <code>binaryFileNode</code>
     * and added to the header value. The property in which the filename is stored should be configured using
     * <code>contentDispositionFilenameProperty</code>.
     *
     * @param request             HTTP request
     * @param response            HTTP response to set header in
     * @param responseContentType content type of the binary file
     * @param binaryFileNode      the node representing the binary file that is streamed to the client
     * @throws javax.jcr.RepositoryException when something goes wrong during repository access
     */
    public static void addContentDispositionHeader(HttpServletRequest request, HttpServletResponse response,
            String fileName, String contentDispositionFileNameEncoding) {

        // The response content type matches one of the configured content types so add a Content-Disposition
        // header to the response
        StringBuilder headerValue = new StringBuilder("attachment");

        // A filename is set for the binary node, so add this to the Content-Disposition header value
        if (!StringUtils.isBlank(fileName)) {
            String encodedFilename = encodeFileName(request, response, fileName, contentDispositionFileNameEncoding);
            headerValue.append("; filename=\"").append(encodedFilename).append("\"");
        }

        response.addHeader("Content-Disposition", headerValue.toString());
    }

    /**
     * <p>When the <code>fileName</code> consists only of US-ASCII chars, we can safely return the <code>fileName</code> <b>as is</b>. However, when the  <code>fileName</code>
     * does contains non-ascii-chars there is a problem because of different browsers expect different encoding: there is no uniform version that works for 
     * all browsers. So, we are either stuck to user-agent sniffing to return the correct encoding, or try to return a US-ASCII form as best as we can.</p>
     *  
     * <p>The problem with user-agent sniffing is that in general, when you use reverse proxies, you do not want to cache pages <b>per</b> browser type. If one version
     * is demanded for all different user agents, the best we can do is trying to bring the fileName back to its base form, thus, replacing diacritics by their
     * base form. This will work fine for most Latin alphabets.</p> 
     * 
     * <p>However, a language like Chinese won't be applicable for this approach. The only way to have it correct in such languages, is to return 
     * a different version for different browsers</p>
     * 
     * <p>To be able to serve both usecases, we make it optional <b>how</b> you'd like your encoding strategy to be. The default strategy, is assuming
     * Latin alphabets and try to get the non-diacritical version of a fileName: The default strategy is thus (browser) user-agent-agnostic.</p>
     * 
     * <p>For languages like Chinese, you can if you do want all browser version to display the correct fileName for the Content-Disposition, tell the 
     * binaries servlet to do so with the following init-param. Note that in this case, you might have to account for the user-agent in your reverse proxy setup</p>
     * 
     * <pre>
     * &lt;init-param&gt;
     *     &lt;param-name&gt;contentDispositionFilenameEncoding&lt;/param-name&gt;
     *     &lt;param-value&gt;user-agent-specific&lt;/param-value&gt;
     * &lt;/init-param&gt;
     * </pre>
     * 
     * @param request
     * @param response
     * @param fileName the un-encoded filename
     * @return
     */
    public static String encodeFileName(HttpServletRequest request, HttpServletResponse response, String fileName,
            String contentDispositionFileNameEncoding) {

        try {
            String responseEncoding = response.getCharacterEncoding();
            String encodedFileName = URLEncoder.encode(fileName, responseEncoding != null ? responseEncoding
                    : DEFAULT_ENCODING);

            if (encodedFileName.equals(fileName)) {
                log.debug("The filename did not contains non-ascii chars: we can safely return an un-encoded version");
                return fileName;
            }

            if (USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING.equals(contentDispositionFileNameEncoding)) {
                return getUserAgentAgnosticFileName(fileName, responseEncoding);
            } else if (USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING
                    .equals(contentDispositionFileNameEncoding)) {
                String userAgent = request.getHeader("User-Agent");
                return getUserAgentSpecificFileName(fileName, responseEncoding, userAgent);
            } else {
                log.warn("Invalid encoding strategy: only allowed is '"
                        + USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING + "' or '"
                        + USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING + "'. Return " + DEFAULT_ENCODING
                        + " encoded version.");
                return URLEncoder.encode(fileName, responseEncoding != null ? responseEncoding : DEFAULT_ENCODING);
            }

        } catch (UnsupportedEncodingException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to encode filename.", e);
            } else {
                log.warn("Failed to encode filename. {}", e.toString());
            }
        }

        return fileName;
    }

    private static String getUserAgentAgnosticFileName(String fileName, String responseEncoding)
            throws UnsupportedEncodingException {
        // let's try to bring the filename to it's baseform by replacing diacritics: if then still there are non-ascii chars, we log an info 
        // message that you might need user-agent-specific mode, and return a utf-8 encoded version. 

        String asciiFileName = EncodingUtils.isoLatin1AccentReplacer(fileName);

        // now check whether the asciiFileName really only contains ascii chars:
        String encodedAsciiFileName = URLEncoder.encode(asciiFileName, responseEncoding != null ? responseEncoding
                : DEFAULT_ENCODING);
        if (encodedAsciiFileName.equals(asciiFileName)) {
            log.debug("Replaced fileName '{}' with its un-accented equivalent '{}'", fileName, asciiFileName);
            return asciiFileName;
        } else {
            log.info("Filename '{}' consists of non latin chars. We have to utf-8 encode the filename, "
                    + " which might be shown with unencoded in some browsers."
                    + " If you want to avoid this, use '{}'. However, this influences reverse proxies.", fileName,
                    USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING);
            return encodedAsciiFileName;
        }
    }

    private static String getUserAgentSpecificFileName(String fileName, String responseEncoding, String userAgent)
            throws UnsupportedEncodingException {
        if (userAgent != null && (userAgent.contains("MSIE") || userAgent.contains("Opera"))) {
            return URLEncoder.encode(fileName, responseEncoding != null ? responseEncoding : DEFAULT_ENCODING);
        } else {
            return EncoderUtil.encodeEncodedWord(fileName, EncoderUtil.Usage.WORD_ENTITY, 0, Charset
                    .forName(DEFAULT_ENCODING), EncoderUtil.Encoding.B);
        }

    }
}