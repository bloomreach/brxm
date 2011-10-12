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

/**
 * All common behavioral node types and property names
 */
public interface BehavioralNodeTypes {

    public final static String BEHAVIORAL_NODETYPE_CONFIGURATION = "behavioral:configuration";
    public final static String BEHAVIORAL_NODETYPE_PROVIDERS = "behavioral:providers";
    public final static String BEHAVIORAL_NODETYPE_PROVIDER = "behavioral:provider";
    public final static String BEHAVIORAL_NODETYPE_RULE = "behavioral:rule";
    public final static String BEHAVIORAL_NODETYPE_PERSONAS = "behavioral:personas";
    public final static String BEHAVIORAL_NODETYPE_PERSONA = "behavioral:persona";
    
    public final static String BEHAVIORAL_GENERAL_PROPERTY_NAME =  "behavioral:name";

    public final static String BEHAVIORAL_PROVIDER_PROPERTY_CLASSNAME = "behavioral:className";
    public final static String BEHAVIORAL_PROVIDER_PROPERTY_SIZE = "behavioral:size";
    public final static String BEHAVIORAL_PROVIDER_PROPERTY_ = "behavioral:className";

    public final static String BEHAVIORAL_PROVIDER_TERMS_PROPERTY = "behavioral:termsProperty";
    public final static String BEHAVIORAL_PROVIDER_QUERY_PARAMETER_PROPERTY = "behavioral:queryParameter";
    
    public final static String BEHAVIORAL_RULE_PROPERTY_TERMS = "behavioral:terms";
    public final static String BEHAVIORAL_RULE_PROPERTY_WEIGHT = "behavioral:weight";
    public final static String BEHAVIORAL_RULE_PROPERTY_FREQUENCY_THRESHOLD = "behavioral:frequencyThreshold";
    public final static String BEHAVIORAL_RULE_PROPERTY_PROVIDER ="behavioral:provider";
    
}
