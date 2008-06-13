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
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static void main(String[] args) {
        ReportGenerator reportGenerator = new ReportGenerator();
        reportGenerator.execute(args);
    }

    private File historyDir;
    private File reportsDir;

    void execute(String[] args) {
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i] == null ? "" : args[i];
        }
        parseCommandLine(args);
        File[] historyFiles = historyDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith("-history.xml");
            }
        });

        ChartsCreator chartsCreator = new ChartsCreator();
        chartsCreator.createCharts(historyFiles, reportsDir);
    }

    @SuppressWarnings("static-access")
    private void parseCommandLine(String[] args) {
        try {
            Options options = new Options();

            Option history = OptionBuilder.withArgName("directory").hasArg()
                    .withDescription("History points directory").withLongOpt("history").create("h");
            options.addOption(history);

            Option report = OptionBuilder.withArgName("directory").hasArg().withDescription("History report directory")
                    .withLongOpt("report").create("r");
            options.addOption(report);

            options.addOption("?", "help", false, "Print usage information");

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("?")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Testutils main", options);
                System.exit(0);
            }

            historyDir = new File(cmd.getOptionValue("h"));
            if (!historyDir.isDirectory()) {
                System.err.println("History points directory '" + historyDir + "' does not exist");
                System.exit(1);
            }

            reportsDir = new File(cmd.getOptionValue("r"));
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }
            if (!reportsDir.isDirectory()) {
                System.err.println("Reporting directory '" + reportsDir + "' is not a directory");
                System.exit(1);
            }

        } catch (ParseException e) {
            System.err.println("Error while parsing commandline: " + e.getMessage());
            System.exit(-1);
        }
    }

}
