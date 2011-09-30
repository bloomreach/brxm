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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BehavioralProfileCalculator {

    private static final Logger log = LoggerFactory.getLogger(BehavioralProfileCalculator.class);
    
    private final Configuration configuration;
    
    public BehavioralProfileCalculator(Configuration configuration) {
        this.configuration = configuration;
    }
    
    public BehavioralProfile calculate(List<BehavioralData> behavioralDataList) {
        
        Map<String, Map<String, BehavioralSegmentScore>> dimensionSegmentScoreMap = new HashMap<String, Map<String, BehavioralSegmentScore>>();
        
        // calculate and collect the absolute segment scores
        for(BehavioralData behavioralData : behavioralDataList) {
            final Map<String, Integer> termFreqs = behavioralData.getTermFreq();
            final BehavioralDataProvider provider = configuration.getDataProvider(behavioralData.getProviderId());
            if (log.isDebugEnabled()) {
                log.debug("processing data from provider " + behavioralData.getProviderId());
            }
            for(Entry<String, Integer> termFreq : termFreqs.entrySet()) {
                final String term = termFreq.getKey();
                final Integer frequency = termFreq.getValue();
                
                // the rules for a term
                if (log.isDebugEnabled()) {
                    log.debug("term " + term + " was counted a total of " + frequency + " times");
                }

                final List<Rule> rules = configuration.getTermsToRules().get(term);
                if(rules == null) {
                    continue;
                }
                
                for(Rule rule : rules) {
                    if (rule.getProviderId().equals(behavioralData.getProviderId())) {
                        final Long providerWeight = provider != null ? provider.getWeight() : 1L;
                        final Long ruleWeight = rule.getWeight();
                        final Long score = ruleWeight * providerWeight * frequency;
                        
                        if (log.isDebugEnabled()) {
                            log.debug("term " + term + " scoring a total of " + ruleWeight + "(rule weight) * " + providerWeight + "(provider weight) * " + frequency + "(term frequency) = " + score);
                        }
                        
                        final String dimensionId = rule.getSegment().getDimension().getId();
                        Map<String, BehavioralSegmentScore> segmentIdScoreMap = dimensionSegmentScoreMap.get(dimensionId);
                        if(segmentIdScoreMap == null) {
                            segmentIdScoreMap = new HashMap<String, BehavioralSegmentScore>();
                            dimensionSegmentScoreMap.put(dimensionId, segmentIdScoreMap);
                        }

                        final String segmentId = rule.getSegment().getId();
                        BehavioralSegmentScore segmentScore = segmentIdScoreMap.get(segmentId);
                        if(segmentScore == null) {
                            segmentScore = new BehavioralSegmentScore(segmentId, score);
                            segmentIdScoreMap.put(segmentId, segmentScore);
                        } else {
                            segmentScore.addAbsoluteScore(score);
                        }
                    }
                }            
            }
        }
        
        // calculate the relative segment scores
        for (Map<String, BehavioralSegmentScore> segmentScores : dimensionSegmentScoreMap.values()) {
            Long totalScore = 0L;
            for (BehavioralSegmentScore segment : segmentScores.values()) {
                totalScore += segment.getAbsoluteScore();
            }
            for (BehavioralSegmentScore segmentScore : segmentScores.values()) {
                final Double relativeScore = segmentScore.getAbsoluteScore().doubleValue() / totalScore.doubleValue();
                segmentScore.setScore(relativeScore);
            }
        }
        
        // collect the absolute persona scores
        Map<String, BehavioralPersonaScore> personaScores = new HashMap<String, BehavioralPersonaScore>();
        Double totalScore = 0.0;
        for (Persona persona : configuration.getPersonas().values()) {
            log.debug("Calculating absolute score for persona " + persona.getId());
            BehavioralPersonaScore personaScore = personaScores.get(persona.getId());
            if (personaScore == null) {
                personaScore = new BehavioralPersonaScoreImpl(persona.getId());
                personaScores.put(persona.getId(), personaScore);
            }
            for (Segment segment : persona.getSegments()) {
                log.debug("Processing segment " + segment.getId());
                Map<String, BehavioralSegmentScore> segmentNameScoreMap = dimensionSegmentScoreMap.get(segment.getDimension().getId());
                if (segmentNameScoreMap != null) {
                    BehavioralSegmentScore segmentScore = segmentNameScoreMap.get(segment.getId());
                    if (segmentScore != null) {
                        log.debug("Found score: " + segmentScore.getScore());
                        ((BehavioralPersonaScoreImpl) personaScore).addAbsoluteScore(segmentScore.getScore());
                        continue;
                    }
                }
                ((BehavioralPersonaScoreImpl) personaScore).addAbsoluteScore(0.0);
            }
            totalScore += ((BehavioralPersonaScoreImpl) personaScore).getAbsoluteScore();
        }
        
        // calculate the relative persona scores
        for (BehavioralPersonaScore personaScore : personaScores.values()) {
            Double relativeScore = 0.0;
            if (totalScore > 0.0) {
                relativeScore = ((BehavioralPersonaScoreImpl) personaScore).getAbsoluteScore().doubleValue() / totalScore.doubleValue();
            }
            ((BehavioralPersonaScoreImpl) personaScore).setScore(relativeScore);
        }
        
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nsegment scores:\n");
            for (Entry<String, Map<String, BehavioralSegmentScore>> dimension : dimensionSegmentScoreMap.entrySet()) {
                for (Entry<String, BehavioralSegmentScore> segment : dimension.getValue().entrySet()) {
                    sb.append(" segment: " + segment.getKey() + ", score: " + segment.getValue().getScore() + "\n");
                }
            }
            sb.append("persona scores:\n");
            for (BehavioralPersonaScore personaScore : personaScores.values()) {
                sb.append(" persona: " + personaScore.getPersonaId() + ", score: " + personaScore.getScore() + "\n");
            }
            log.debug(sb.toString());
        }
        
        return new BehavioralProfileImpl(personaScores);
    }

}
