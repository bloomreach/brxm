/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.googleanalytics;

import javax.jcr.RepositoryException;
import java.io.InputStream;

public interface GoogleAnalyticsService {

    /**
     * @return  configured google analytics account id or {@code null} if not configured
     */
    String getAccountId();

    /**
     * @return  configured google analytics table id or {@code null} if not configured
     */
    String getTableId();

    /**
     * @return  configured google analytics account password or {@code null} if not configured
     */
    @Deprecated
    String getPassword();

    /**
     * @return  configured google analytics account user name or {@code null} if not configured
     */
    String getUserName();

    /**
     *
     * @return  input stream to uploaded key file, {@code PKCS 12}
     * @throws RepositoryException
     */
    InputStream getPrivateKey() throws RepositoryException;

}
