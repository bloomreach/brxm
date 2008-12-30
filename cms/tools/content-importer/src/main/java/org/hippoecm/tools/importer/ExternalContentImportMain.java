/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.tools.importer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.tools.importer.api.ImportException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class ExternalContentImportMain {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String DEFAULT_CONFIG = "import.properties";
    private static Configuration config;

    private static Logger log = LoggerFactory.getLogger(ExternalContentImportMain.class);

    private ExternalContentImportMain() {
    }

    /**
     * @param args
     * @throws RepositoryException
     * @throws IOException
     */
    public static void main(String[] args) throws ImportException {

        if (log.isDebugEnabled()) {
            log.debug("Content import starting.");
        }
        parseCommandLine(args);
        try {
            log.info("Content import configured and started.");
            ExternalContentImport eci = new ExternalContentImport(config);
        } finally {
            log.info("Content import finished.");
        }
    }



    /**
     * Parse the commandline arguments.
     */
    private static void parseCommandLine(String[] args) {

        if (log.isDebugEnabled()) {
            log.debug("Parse command line.");
        }

        try {
            Options options = new Options();
            Option configOption = OptionBuilder.withArgName( "config" ).hasArg().withDescription("Configuration file (default '" + DEFAULT_CONFIG + "')").create( "config");
            options.addOption(configOption);
            options.addOption("h", "help", false, "Print usage information");

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);

            List argList= cmd.getArgList();

            boolean valid = true;

            String configFile = cmd.getOptionValue("config", DEFAULT_CONFIG);
            File f = new File(configFile);
            if (!f.exists()) {
                log.error("Config file not found: " + configFile);
                valid = false;
            } else {
                try {
                    config = new PropertiesConfiguration(configFile);
                } catch (ConfigurationException e) {
                    log.error("Invalid confuration file: " + configFile);
                    valid = false;
                }
            }
            if (!valid || cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(ExternalContentImportMain.class.getName()+ " ", options, true);
                System.exit(0);
            }


        } catch (ParseException e) {
            System.err.println("Error while parsing commandline: " + e.getMessage());
            System.exit(-1);
        }

        if (log.isDebugEnabled()) {
            log.debug("Command line parsed.");
        }
    }
}
