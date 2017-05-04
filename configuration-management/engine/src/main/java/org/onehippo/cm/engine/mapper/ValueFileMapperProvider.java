/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.mapper;

import org.onehippo.cm.api.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Extension scanner. Loads all available mappers
 */
public class ValueFileMapperProvider {

    private static final Logger log = LoggerFactory.getLogger(ValueFileMapperProvider.class);

    private static ValueFileMapperProvider instance;

    final List<ValueFileMapper> valueFileMappers = new ArrayList<>();

    private final ValueFileMapper defaultMapper = new DefaultFileMapper();

    public static ValueFileMapperProvider getInstance() {
        if (instance == null) {
            instance = new ValueFileMapperProvider();
        }
        return instance;
    }

    private ValueFileMapperProvider() {
        valueFileMappers.addAll(loadAvailableMappers());
    }

    /**
     * Get smart name from available mappers or default one
     * @param value
     * @return Best matching filename
     */
    public String generateSmartName(Value value) {

        return valueFileMappers.stream().map(x -> x.apply(value))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(getShortestString())
                .findFirst()
                .orElse(defaultMapper.apply(value).get());
    }

    private Comparator<String> getShortestString() {
        return (e2, e1) -> e1.length() > e2.length() ? -1 : 1;
    }

    /**
     * @return Loads list of all available mappers available at current class's package
     */
    private List<ValueFileMapper> loadAvailableMappers() {

        final List<ValueFileMapper> list = new ArrayList<>();

        final String packageName = this.getClass().getPackage().getName();
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(ValueFileMapper.class));
        provider.addExcludeFilter(new AssignableTypeFilter(DefaultFileMapper.class));

        final Set<BeanDefinition> classes = provider.findCandidateComponents(packageName);
        for (BeanDefinition beanDefinition : classes) {
            final String className = beanDefinition.getBeanClassName();
            try {
                final Class<?> clazz = Class.forName(className);
                final boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
                if (!isAbstract) {
                    final ValueFileMapper mapper = (ValueFileMapper) clazz.newInstance();
                    list.add(mapper);
                }
            } catch (Exception ignored) {
                log.debug(String.format("Instantiating '%s' class failed", className));
            }
        }
        return list;
    }
}
