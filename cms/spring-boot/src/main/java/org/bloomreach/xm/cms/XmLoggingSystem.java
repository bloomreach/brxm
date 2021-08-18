/*
 *  Copyright 2021 Bloomreach
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
package org.bloomreach.xm.cms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.net.ssl.LaxHostnameVerifier;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

public class XmLoggingSystem extends Log4J2LoggingSystem {
    private static final String HTTPS = "https";
    public static final String ENVIRONMENT_KEY = "SpringEnvironment";
    private static Logger LOGGER = StatusLogger.getLogger();

    public XmLoggingSystem(ClassLoader loader) {
        super(loader);
    }

    /**
     * Set the environment into the ExternalContext field so that it can be obtained by SpringLookup when it
     * is constructed. Spring will replace the ExternalContext field with a String once initialization is
     * complete.
     * @param initializationContext The initialization context.
     * @param configLocation The configuration location.
     * @param logFile the log file.
     */
    @Override
    public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
        getLoggerContext().putObjectIfAbsent(ENVIRONMENT_KEY, initializationContext.getEnvironment());
        super.initialize(initializationContext, configLocation, logFile);
    }

    @Override
    protected String[] getStandardConfigLocations() {
        String[] locations = super.getStandardConfigLocations();
        PropertiesUtil props = new PropertiesUtil(new Properties());
        String location = props.getStringProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        if (location != null) {
            List<String> list = new ArrayList<>();
            list.addAll(Arrays.asList(super.getStandardConfigLocations()));
            list.add(location);
            locations = list.toArray(new String[0]);
        }
        return locations;
    }

    @Override
    protected void loadConfiguration(String location, LogFile logFile) {
        Assert.notNull(location, "Location must not be null");
        try {
            LoggerContext ctx = getLoggerContext();
            String[] locations = parseConfigLocations(location);
            if (locations.length == 1) {
                final URL url = ResourceUtils.getURL(location);
                final ConfigurationSource source = getConfigurationSource(url);
                if (source != null) {
                    ctx.start(ConfigurationFactory.getInstance().getConfiguration(ctx, source));
                }
            } else {
                final List<AbstractConfiguration> configs = new ArrayList<>();
                for (final String sourceLocation : locations) {
                    final ConfigurationSource source = getConfigurationSource(ResourceUtils.getURL(sourceLocation));
                    if (source != null) {
                        final Configuration config = ConfigurationFactory.getInstance().getConfiguration(ctx, source);
                        if (config instanceof AbstractConfiguration) {
                            configs.add((AbstractConfiguration) config);
                        } else {
                            LOGGER.warn("Configuration at {} cannot be combined in a CompositeConfiguration", sourceLocation);
                            return;
                        }
                    }
                }
                if (configs.size() > 1) {
                    ctx.start(new CompositeConfiguration(configs));
                } else {
                    ctx.start(configs.get(0));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                "Could not initialize Log4J2 logging from " + location, ex);
        }
    }

    @Override
    public void cleanUp() {
        getLoggerContext().removeObject(ENVIRONMENT_KEY);
        super.cleanUp();
    }

    private String[] parseConfigLocations(String configLocations) {
        final String[] uris = configLocations.split("\\,");
        final List<String> locations = new ArrayList<>();
        if (uris.length > 1) {
            Arrays.stream(uris).forEach(uri -> locations.add(uri));
            return locations.toArray(new String[0]);
        }
        return new String[] {uris[0]};
    }

    private ConfigurationSource getConfigurationSource(URL url) throws IOException, URISyntaxException {
        URLConnection urlConnection = url.openConnection();
        AuthorizationProvider provider = ConfigurationFactory.authorizationProvider(PropertiesUtil.getProperties());
        provider.addAuthorization(urlConnection);
        if (url.getProtocol().equals(HTTPS)) {
            SslConfiguration sslConfiguration = SslConfigurationFactory.getSslConfiguration();
            if (sslConfiguration != null) {
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslConfiguration.getSslSocketFactory());
                if (!sslConfiguration.isVerifyHostName()) {
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(LaxHostnameVerifier.INSTANCE);
                }
            }
        }
        File file = FileUtils.fileFromUri(url.toURI());
        try {
            if (file != null) {
                return new ConfigurationSource(urlConnection.getInputStream(), FileUtils.fileFromUri(url.toURI()));
            } else {
                return new ConfigurationSource(urlConnection.getInputStream(), url, urlConnection.getLastModified());
            }
        } catch (FileNotFoundException ex) {
            LOGGER.info("Unable to locate file {}, ignoring.", url.toString());
            return null;
        }
    }

    private LoggerContext getLoggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }

}
