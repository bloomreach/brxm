/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package org.hippoecm.testutils.history;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class ReportGenerator {

    private static final String DEFAULT_HISTORY = "target/history";
    private static final String DEFAULT_REPORTS = "target/history-reports";

    public static void main(String[] args) {
        ReportGenerator reportGenerator = new ReportGenerator();
        reportGenerator.execute(args);
    }

    private File historyDir;
    private File reportsDir;

    void execute(String[] args) {
        parseCommandLine(args);
        File[] historyFiles = historyDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith("-history.xml");
            }
        });
        reportsDir = new File(DEFAULT_REPORTS);
        
        ChartsCreator chartsCreator = new ChartsCreator();
        chartsCreator.createCharts(historyFiles, reportsDir );
    }

    @SuppressWarnings("static-access")
    private void parseCommandLine(String[] args) {
        try {
            Options options = new Options();

            Option h = OptionBuilder.withArgName("directory").hasArg().withDescription("History points directory")
                    .withLongOpt("history").create("h");
            options.addOption(h);
            options.addOption("?", "help", false, "Print usage information");

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("?")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Testutils main", options);
                System.exit(0);
            }

            historyDir = new File(cmd.getOptionValue("h", DEFAULT_HISTORY));
            if (!historyDir.isDirectory()) {
                System.err.println("Directory '" + historyDir + "' does not exist");
                System.exit(1);
            }

        } catch (ParseException e) {
            System.err.println("Error while parsing commandline: " + e.getMessage());
            System.exit(-1);
        }
    }

}
