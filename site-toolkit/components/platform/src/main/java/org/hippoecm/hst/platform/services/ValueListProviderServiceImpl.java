/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.platform.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.platform.api.ValueListProviderService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueListProviderServiceImpl implements ValueListProviderService {

    private Map<String, ValueListProvider> providers = new ConcurrentHashMap<>();
    private Map<String, String> providerDefinitions;
    private final static Logger log = LoggerFactory.getLogger(ValueListProviderServiceImpl.class);

    ValueListProviderServiceImpl(
        final Map<String, String> providers, final Map<String, String> customProviders) {

        this.providerDefinitions = new HashMap<>();
        this.providerDefinitions.putAll(providers);
        customProviders.entrySet().stream().forEach(entry -> {
            String providerName = entry.getKey();
            String providerClass = entry.getValue();
            if (providerDefinitions.containsKey(providerName)) {
                log.info("The provider with name {} and class {} is going to be overrided with class {}", providerName,
                    providerDefinitions.get(providerName), providerClass);
            }
            providerDefinitions.put(providerName, providerClass);
        });
        this.providerDefinitions.putAll(customProviders);
        this.providerDefinitions = Collections.unmodifiableMap(this.providerDefinitions);
    }

    @Override
    public ValueListProvider getProvider(final String name) {
        return getValueListProvider(name, null);
    }

    @Override
    public ValueListProvider getProvider(final String name, final String sourceId) {
        return getValueListProvider(name, sourceId);
    }

    @Nullable
    private ValueListProvider getValueListProvider(final String name, final String sourceId) {
        boolean useSourceId = StringUtils.isNotBlank(sourceId);
        String compoundKey = name + (useSourceId?  "#" + sourceId: "");
        ValueListProvider valueListProvider = providers.get(compoundKey);
        if (valueListProvider == null) {
            String providerClassName = providerDefinitions.get(name);
            try {
                valueListProvider = (ValueListProvider) (useSourceId?
                    Class.forName(providerClassName).getDeclaredConstructor(String.class).newInstance(sourceId) :
                    Class.forName(providerClassName).getDeclaredConstructor().newInstance());

                if (valueListProvider != null) {
                    providers.put(compoundKey, valueListProvider);
                }
            } catch (Exception e) {
                log.error("It wasn't possible to retrieve a value list provider for the key " + compoundKey, e);
            }
        }
        return valueListProvider;
    }
}