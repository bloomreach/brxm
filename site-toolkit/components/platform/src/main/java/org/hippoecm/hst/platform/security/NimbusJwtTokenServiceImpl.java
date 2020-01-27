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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.hippoecm.hst.container.security.AccessToken;
import org.hippoecm.hst.container.security.InvalidTokenException;
import org.hippoecm.hst.container.security.JwtTokenService;
import org.hippoecm.hst.container.security.TokenException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

public class NimbusJwtTokenServiceImpl implements JwtTokenService {

    private final JWSAlgorithm JWS_ALGORITHM = JWSAlgorithm.RS256;

    private RSAKey rsaJWK;
    private JWSSigner signer;
    private JWSVerifier verifier;
    private boolean registered;

    TokenCmsSessionContextRegistry registry = new TokenCmsSessionContextRegistry();

    public void init() {
        try {
            // Create RSA-key
            rsaJWK = new RSAKeyGenerator(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
                    .algorithm(JWS_ALGORITHM) // specify the signing algorithm
                    .generate();
            // Create RSA-signer
            signer = new RSASSASigner(rsaJWK);
            // Create RSA-verifier
            verifier = new RSASSAVerifier(rsaJWK);
            HippoServiceRegistry.register(this, JwtTokenService.class);
            registered = true;
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        if (registered) {
            registered = false;
            HippoServiceRegistry.unregister(this, JwtTokenService.class);
            registry.clearRegistry();
            verifier = null;
            signer = null;
            rsaJWK = null;
        }
    }

    @Override
    public String createToken(final HttpServletRequest request, final Map<String, Object> claims) {
        if (!registered) {
            throw new IllegalStateException("Service not initialized");
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("Cannot create jwt token for unauthenticated users");
        }
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
        if (cmsSessionContext == null) {
            throw new IllegalStateException("Cannot create jwt token for unauthenticated users");
        }
        final String tokenSubject = cmsSessionContext.getId();

        final JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
        claims.forEach(claimsSetBuilder::claim);
        // ensure subject is not (accidentally) overwritten by custom claims by adding it last
        claimsSetBuilder.subject(tokenSubject);

        final SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWS_ALGORITHM), claimsSetBuilder.build());

        // sign
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalArgumentException("JWT could not be signed", e);
        }

        registry.register(tokenSubject, cmsSessionContext, session);

        // To serialize to compact form, produces something like
        // eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
        // mZq3ivwoAjqa1uUkSBKFIX7ATndFF5ivnt-m8uApHO4kfIFOrW7w2Ezmlg3Qd
        // maXlS9DhN0nUk_hGI3amEjkKd0BWYCB8vfUbUv0XGjQip78AI4z1PrFRNidm7
        // -jPDm5Iq0SZnjKjCNS5Q15fokXZc8u0A
        return signedJWT.serialize();
    }

    @Override
    public AccessToken getAccessToken(final String jws) throws InvalidTokenException {
        if (!registered) {
            throw new TokenException("TokenService not initialized");
        }
        try {
            final SignedJWT signedJWT = SignedJWT.parse(jws);
            if (!signedJWT.verify(verifier)) {
                throw new InvalidTokenException("Token is not valid");
            }
            final JWSHeader jwsHeader = signedJWT.getHeader();
            // getting the JWTClaimsSet (re)parses the payload, so only do this once!
            final JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            String tokenSubject = jwtClaimsSet.getSubject();
            if (tokenSubject == null) {
                throw new InvalidTokenException("Token did not contain mandatory subject ('sub')");
            }

            CmsSessionContext cmsSessionContext = registry.getCmsSessionContext(tokenSubject);
            if (cmsSessionContext == null) {
                throw new InvalidTokenException("Token is not bound to a CmsSessionContext (any more)");
            }

            return new NimbusAccessTokenImpl(jwsHeader, jwtClaimsSet, cmsSessionContext);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (ParseException | IllegalStateException | JOSEException e) {
            // signedJWT.verify throws IllegalStateException if jws in not in signed or verified state
            throw new InvalidTokenException("Invalid token");
        } catch (Exception e) {
            throw new TokenException("Token exception happened", e);
        }
    }
}
