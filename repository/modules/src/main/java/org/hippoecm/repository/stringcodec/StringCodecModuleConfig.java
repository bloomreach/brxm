    /*
     * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
    package org.hippoecm.repository.stringcodec;

    import java.util.Collections;
    import java.util.HashMap;
    import java.util.Map;

    import javax.jcr.Node;
    import javax.jcr.Property;
    import javax.jcr.PropertyIterator;
    import javax.jcr.PropertyType;
    import javax.jcr.RepositoryException;

    import org.hippoecm.repository.api.StringCodec;
    import org.hippoecm.repository.api.StringCodecFactory;
    import org.hippoecm.repository.util.JcrUtils;
    import org.slf4j.Logger;

    import static org.slf4j.LoggerFactory.getLogger;

    class StringCodecModuleConfig {

        private static final StringCodec IDENTITY_ENCODING = new StringCodecFactory.IdentEncoding();
        private static final Logger log = getLogger(StringCodecModuleConfig.class);

        private StringCodecFactory codecFactory;

        StringCodecModuleConfig() {
            codecFactory = new StringCodecFactory(Collections.singletonMap(null, IDENTITY_ENCODING));
        }

        void reconfigure(final Node node) {
            try {
                final Map<String, StringCodec> codecMap = new HashMap<>();

                forStringCodecProperties(node, (property) -> {
                    final String className = property.getString();
                    try {
                        final StringCodec stringCodec = loadStringCodec(className);
                        codecMap.put(property.getName(), stringCodec);
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                        log.error("Failed to load StringCodec class {} specified in {}", className, property.getPath(), e);
                    }
                });

                // add default string codec under key 'null'
                codecMap.put(null, IDENTITY_ENCODING);

                codecFactory = new StringCodecFactory(codecMap);
            } catch (RepositoryException e) {
                log.warn("Failed to reconfigure the StringCodec module {}", JcrUtils.getNodePathQuietly(node), e);
            }
        }

        private void forStringCodecProperties(final Node node, final PropertyVisitor visitor) throws RepositoryException {
            final PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                final Property property = properties.nextProperty();
                if (!property.isMultiple()
                        && property.getType() == PropertyType.STRING
                        && !property.getName().contains(":")) {
                    visitor.visit(property);
                }
            }
        }

        private StringCodec loadStringCodec(final String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final Class<? extends StringCodec> clazz = classLoader.loadClass(className).asSubclass(StringCodec.class);
            return clazz.newInstance();
        }

        StringCodecFactory getStringCodecFactory() {
            return codecFactory;
        }
    }
