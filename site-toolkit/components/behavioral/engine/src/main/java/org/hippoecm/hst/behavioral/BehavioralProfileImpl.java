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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BehavioralProfileImpl implements BehavioralProfile {

    private final Map<String, BehavioralPersonaScore> personaScores;
    
    public BehavioralProfileImpl(Map<String, BehavioralPersonaScore> personaScores) {
        this.personaScores = personaScores;
    }

    @Override
    public List<BehavioralPersonaScore> getPersonaScores() {
        BehavioralPersonaScore[] scoresArray = personaScores.values().toArray(new BehavioralPersonaScore[personaScores.size()]);
        Arrays.sort(scoresArray);
        return Arrays.asList(scoresArray);
    }

    @Override
    public String getPrincipalPersonaId() {
        BehavioralPersonaScore principalPersonaScore = getPrincipalPersonaScore();
        if (principalPersonaScore != null) {
            return principalPersonaScore.getPersonaId();
        }
        return null;
    }

    @Override
    public BehavioralPersonaScore getPrincipalPersonaScore() {
        if (personaScores.values().size() > 0) {
            return getPersonaScores().get(0);
        }
        return null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Profile: \n");
        for (BehavioralPersonaScore personaScore : personaScores.values()) {
            sb.append(" persona: " + personaScore.getPersonaId() + ", score: " + personaScore.getScore() + "\n");
        }
        return sb.toString();
    }

}
