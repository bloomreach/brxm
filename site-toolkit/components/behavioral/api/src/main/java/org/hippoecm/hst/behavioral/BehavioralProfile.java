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

import java.util.List;
import java.util.Set;

/**
 * A {@link BehavioralProfile} contains the behavioral information extracted from the current user
 * according to what browse behavior she exhibited. Based on this information a user can be
 * categorized as being one of several configured persona's.
 * <p>
 * A client of this class can inspect how a user scores on the different persona's.
 * </p>
 */
public interface BehavioralProfile {
    
    /**
     * 
     * @return  the list of all {@link BehavioralPersonaScore}s sorted from highest to lowest score. Also {@link BehavioralPersonaScore}s that have
     * a {@link BehavioralPersonaScore#getScore()} equal to 0 are in this list 
     */
    List<BehavioralPersonaScore> getPersonaScores();
    
    Set<String> getPersonas();
    
    BehavioralPersonaScore getPersonaScore(String personaId);
    
}
