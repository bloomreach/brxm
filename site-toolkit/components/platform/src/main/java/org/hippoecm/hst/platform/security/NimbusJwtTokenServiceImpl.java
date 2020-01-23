/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.security;

import java.text.ParseException;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import org.hippoecm.hst.container.security.AccessToken;
import org.hippoecm.hst.container.security.InvalidTokenException;
import org.hippoecm.hst.container.security.JwtTokenService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import net.minidev.json.JSONObject;

public class NimbusJwtTokenServiceImpl implements JwtTokenService {

    private RSAKey rsaJWK;
    private RSAKey rsaPublicJWK;
    private JWSSigner signer;
    private JWSVerifier verifier;

    private boolean registered;

    private TokenCmsSessionContextRegistry registry = new TokenCmsSessionContextRegistry();

    private void init() {
        try {
            rsaJWK = new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                    .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                    .generate();
            rsaPublicJWK = rsaJWK.toPublicJWK();
            // Create RSA-signer with the private key
            signer = new RSASSASigner(rsaJWK);

            verifier = new RSASSAVerifier(rsaPublicJWK);

            HippoServiceRegistry.register(this, JwtTokenService.class);
            registered = true;
        } catch (JOSEException e) {
            e.printStackTrace();
        }


    }

    private void destroy() {
        if (registered) {
            HippoServiceRegistry.unregister(this, JwtTokenService.class);
        }
    }


    @Override
    public String createToken(final HttpServletRequest request, final Map<String, Object> claims) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("Cannot create jwt token for unauthenticated users");
        }
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
        if (cmsSessionContext == null) {
            throw new IllegalStateException("Cannot create jwt token for unauthenticated users");
        }


        JSONObject jsonObject = new JSONObject(claims);
        final String tokenSubject = cmsSessionContext.getId();
        // Prepare JWS object with simple string as payload
        JWSObject jwsObject;
        try {
            jwsObject = new JWSObject(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).customParam("sub", tokenSubject).build(),
                    new Payload(jsonObject));

            // Compute the RSA signature
            jwsObject.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalArgumentException("JWT could not be signed", e);
        }


        registry.register(tokenSubject, cmsSessionContext, session);

        // To serialize to compact form, produces something like
        // eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
        // mZq3ivwoAjqa1uUkSBKFIX7ATndFF5ivnt-m8uApHO4kfIFOrW7w2Ezmlg3Qd
        // maXlS9DhN0nUk_hGI3amEjkKd0BWYCB8vfUbUv0XGjQip78AI4z1PrFRNidm7
        // -jPDm5Iq0SZnjKjCNS5Q15fokXZc8u0A
        return jwsObject.serialize();
    }

    @Override
    public AccessToken getAccessToken(final String jws) throws InvalidTokenException {

        try {
            final JWSObject jwsObject = JWSObject.parse(jws);
            if (!jwsObject.verify(verifier)) {
                throw new InvalidTokenException("Token is not valid");
            }
            Object sub = jwsObject.getHeader().getCustomParam("sub");
            if (sub == null || !(sub instanceof String)) {
                throw new InvalidTokenException("Token did not contain a 'sub' which is mandatory");
            }

            CmsSessionContext cmsSessionContext = registry.getCmsSessionContext((String)sub);
            if (cmsSessionContext == null) {
                throw new InvalidTokenException("Token is not bound to a CmsSessionContext (any more)");
            }

            return new NimbusAccessTokenImpl(jwsObject, cmsSessionContext);
        } catch (ParseException | JOSEException e) {
            throw new InvalidTokenException("Invalid token");
        }

    }
}
