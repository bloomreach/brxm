/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.bloomreach.connector.rest;

public class ResourceData {

    private String resourceName;
    private String realm;
    private String authKey;
    private String domainKey = "";
    private String accountId;
    private String fl;
    private String baseUrl;
    private String basePath;
    private String refUrl = "";
    private String url = "test";
    private String requestId = "1234567890";
    private int maxEntriesLocalHeap = 1000;
    private int maxEntriesLocalDisk = 1000;
    private int timeToLiveSeconds = 600;
    private int timeToIdleSeconds = 600;
    private boolean crispExists;


    public String getBasePath() {
        return "/hippo:configuration/hippo:modules/crispregistry/hippo:moduleconfig/crisp:resourceresolvercontainer";
    }

    public int getMaxEntriesLocalHeap() {
        return maxEntriesLocalHeap;
    }

    public void setMaxEntriesLocalHeap(final int maxEntriesLocalHeap) {
        this.maxEntriesLocalHeap = maxEntriesLocalHeap;
    }

    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds(final int timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    public int getMaxEntriesLocalDisk() {
        return maxEntriesLocalDisk;
    }

    public void setMaxEntriesLocalDisk(final int maxEntriesLocalDisk) {
        this.maxEntriesLocalDisk = maxEntriesLocalDisk;
    }

    public int getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }

    public void setTimeToIdleSeconds(final int timeToIdleSeconds) {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    public void setBasePath(final String basePath) {
        this.basePath = "";
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(final String resourceName) {
        this.resourceName = resourceName;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(final String authKey) {
        this.authKey = authKey;
    }

    public String getDomainKey() {
        return domainKey;
    }

    public void setDomainKey(final String domainKey) {
        this.domainKey = domainKey;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(final String accountId) {
        this.accountId = accountId;
    }

    public String getFl() {
        return fl;
    }

    public void setFl(final String fl) {
        this.fl = fl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String toString() {
        return "ResourceData{" +
                "resourceName='" + resourceName + '\'' +
                ", realm='" + realm + '\'' +
                ", authKey='" + authKey + '\'' +
                ", domainKey='" + domainKey + '\'' +
                ", accountId='" + accountId + '\'' +
                ", fl='" + fl + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", basePath='" + getBasePath() + '\'' +
                '}';
    }

    public String getRefUrl() {
        return refUrl;
    }

    public void setRefUrl(final String refUrl) {
        this.refUrl = refUrl;
    }

    public String getUrl() {
        return url == null ? "test" : url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public void setCrispExists(final boolean crispExists) {
        this.crispExists = crispExists;
    }

    public boolean isCrispExists() {
        return crispExists;
    }
}
