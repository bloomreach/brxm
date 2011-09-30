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

public class BehavioralPersonaScoreImpl implements BehavioralPersonaScore, Comparable<BehavioralPersonaScoreImpl> {

    private final String personaId;
    private Double score;
    private Double absoluteScore = 1.0;
    
    public BehavioralPersonaScoreImpl(String personaId) {
        this.personaId = personaId;
    }
    
    @Override
    public String getPersonaId() {
        return personaId;
    }

    @Override
    public Double getScore() {
        return score;
    }
    
    void setScore(Double score) {
        this.score = score;
    }
    
    void addAbsoluteScore(Double absoluteScore) {
        this.absoluteScore *= absoluteScore;
    }
    
    Double getAbsoluteScore() {
        return this.absoluteScore;
    }

    @Override
    public int compareTo(BehavioralPersonaScoreImpl o) {
        if (getScore() > o.getScore()) {
            return -1;
        }
        if (o.getScore() > getScore()) {
            return 1;
        }
        return 0;
    }

}
