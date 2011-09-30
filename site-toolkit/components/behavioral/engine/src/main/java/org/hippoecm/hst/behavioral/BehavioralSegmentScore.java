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
package org.hippoecm.hst.behavioral;

public class BehavioralSegmentScore {

    private final String segmentId;
    private Long absoluteScore;
    private Double score;
    
    public BehavioralSegmentScore(String segmentId, Long absoluteScore) {
        this.segmentId = segmentId;
        this.absoluteScore = absoluteScore;
    }
    
    public String getSegmentId() {
        return segmentId;
    }
    
    public Double getScore() {
        return score;
    }

    void setScore(Double score) {
        this.score = score;
    }

    Long getAbsoluteScore() {
        return absoluteScore;
    }
    
    void addAbsoluteScore(Long absoluteScore) {
        this.absoluteScore += absoluteScore;
    }
     
}
