/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.hst.behavioral;

import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * {@link BehavioralDataStore} interface for storing the {@link BehavioralData}. The storage is pluggable, and
 * can vary from a {@link Cookie} based storage, to {@link HttpSession}, to memory caching, to persistent caches, to clustered caches, to clustered 
 * persisting nosql solutions, etc.
 * </p>
 * <p>
 * <b>Note</b> that {@link BehavioralDataStore} implementations must be thread-safe: {@link #storeBehavioralData(HttpServletRequest, HttpServletResponse, List)} and
 * {@link #readBehavioralData(HttpServletRequest)} are called concurrently by multiple {@link HttpServletRequest}s
 * </p>
 */
public interface BehavioralDataStore {

    /**
     * Stores the {@link List} of {@link BehavioralData} objects 
     * @param request the {@link HttpServletRequest}
     * @param response the {@link HttpServletResponse}
     * @param behavioralDataList the {@link List} of {@link BehavioralData} objects to store. 
     */
    void storeBehavioralData(HttpServletRequest request, HttpServletResponse response, List<BehavioralData> behavioralDataList);
    
    /**
     * Reads the {@link List} of {@link BehavioralData} objects 
     * @param request the {@link HttpServletRequest}
     * @return The {@link List} of {@link BehavioralData} objects and an empty {@link List} when there is no {@link BehavioralData} yet
     */
    List<BehavioralData> readBehavioralData(HttpServletRequest request);
    
}
