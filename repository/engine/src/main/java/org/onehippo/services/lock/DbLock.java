/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.services.lock;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DbLock extends AbstractLock {

    private static final Logger log = LoggerFactory.getLogger(DbLock.class);

    ResultSet dbLockResult;

    DbLock(final String lockKey, final ResultSet dbLockSet) {
        super(lockKey, Thread.currentThread().getName(), System.currentTimeMillis());
        this.dbLockResult = dbLockSet;
    }

    @Override
    void destroy() {
        try {
            dbLockResult.close();
        } catch (SQLException e) {
            log.error("Error while destroying DbLock", e);
        }
    }
}
