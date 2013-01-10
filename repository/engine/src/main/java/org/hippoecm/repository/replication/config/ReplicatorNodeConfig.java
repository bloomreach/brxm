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
package org.hippoecm.repository.replication.config;

import java.util.List;

import org.hippoecm.repository.replication.ReplicatorNode;

/**
 * The replicator node configuration hold the configuration for a {@link ReplicatorNode}.
 */
public class ReplicatorNodeConfig {

    /**
     * Identifier.
     */
    private final String id;

    /**
     * The replicator configuration
     */
    private final ReplicatorConfig replicatorConfig;

    /**
     * The replicator filter configurations.
     */
    private final List<FilterConfig> filterConfigs;

    /**
     * Delay between revision syncs in milliseconds.
     */
    private long syncDelay;

    /**
     * Delay to wait for replicator to stop in milliseconds.
     */
    private long stopDelay;

    /**
     * Delay between replication retries in milliseconds.
     */
    private long retryDelay;

    /**
     * Max number of retries;
     */
    private int maxRetries;

    /**
     * Creates a new replicator configuration.
     *
     * @param id custom cluster node id
     * @param syncDelay syncDelay, in milliseconds
     * @param jc journal configuration
     */
    public ReplicatorNodeConfig(String id, ReplicatorConfig replicatorConfig, List<FilterConfig> filterConfigs) {
        this.id = id;
        this.replicatorConfig = replicatorConfig;
        this.filterConfigs = filterConfigs;
    }

    //----------------- getters and setters -------------//

    public String getId() {
        return id;
    }

    public long getSyncDelay() {
        return syncDelay;
    }

    public void setSyncDelay(long syncDelay) {
        this.syncDelay = syncDelay;
    }

    public long getStopDelay() {
        return stopDelay;
    }

    public void setStopDelay(long stopDelay) {
        this.stopDelay = stopDelay;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(long retryDelay) {
        this.retryDelay = retryDelay;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public ReplicatorConfig getReplicatorConfig() {
        return replicatorConfig;
    }

    public List<FilterConfig> getFilterConfigs() {
        return filterConfigs;
    }
}
