/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container;

import com.fasterxml.jackson.databind.ObjectMapper;

// TODO token handling by https://medium.com/@hantsy/protect-rest-apis-with-spring-security-and-jwt-5fbc90305cc5
public class AccessToken {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Token token;

    // TODO check signing
    public AccessToken(final String accessToken) throws Exception {
        token = objectMapper.readValue(accessToken, Token.class);
    }

    public String getCmsSCID() {
        return token.getCmsSCID();
    }

    public boolean isPreviewRequest() {
        return token.isPreviewRequest();
    }

    public boolean isChannelManagerRequest() {
        return token.isChannelManagerRequest();
    }

    public static class Token {

        private String cmsSCID;
        private boolean previewRequest;
        private boolean channelManagerRequest;

        public String getCmsSCID() {
            return cmsSCID;
        }

        public void setCmsSCID(final String cmsSCID) {
            this.cmsSCID = cmsSCID;
        }

        public boolean isPreviewRequest() {
            return previewRequest;
        }

        public void setPreviewRequest(final boolean previewRequest) {
            this.previewRequest = previewRequest;
        }

        public boolean isChannelManagerRequest() {
            return channelManagerRequest;
        }

        public void setChannelManagerRequest(final boolean channelManagerRequest) {
            this.channelManagerRequest = channelManagerRequest;
        }
    }
}
