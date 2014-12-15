/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.poll.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean representing the voted options for a particular poll, i.e. a poll:polldata node in the repository.
 */
public class PollVotesBean {

    private final List<Option> options = new ArrayList<Option>();
    private long totalVotesCount = 0;

    /**
     * Constructor
     */
    public PollVotesBean() {
        super();
    }

    /**
     * Add a poll option with its number of votes.
     */
    public void addOptionVotes(String value, String label, long votesCount) {

        this.totalVotesCount = totalVotesCount + votesCount;
        this.options.add(new Option(value, label, votesCount));
    }

    /**
     * Get the list of options
     */
    public List<Option> getOptions() {
        return this.options;
    }

    /**
     * Get the total number of the votes of the options, each rounded to long.
     * This enables easy checking if the total number of votes is actually 100%.
     */
    public long getTotalVotesPercentage() {
        long total = 0;
        for (Option o : this.options) {
            total += o.getVotesPercentage();
        }
        return total;
    }

    /**
     * Get the total number of the votes of the options, as double.
     */
    public double getTotalVotesPercentageAsDouble() {
        double total = 0.0;
        for (Option o : this.options) {
            total += o.getVotesPercentageAsDouble();
        }
        return total;
    }

    public class Option {
        private final String value;
        private final String label;
        private final Long votesCount;

        public Option(String value, String label, long votesCount) {
            super();
            this.value = value;
            this.label = label;
            this.votesCount = votesCount;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public long getVotesCount() {
            return this.votesCount;
        }

        /**
         * Get the percentage of votes, rounded to a long.
         */
        public long getVotesPercentage() {
            return Math.round(getVotesPercentageAsDouble());
        }

        /**
         * Get the percentage of votes, as double.
         * This enables presentation in the frontend with decimals in the percentage.
         */
        public double getVotesPercentageAsDouble() {

            if (totalVotesCount == 0) {
                return 0.0;
            }

            double votes = new Long(votesCount).doubleValue();
            double totalVotes = new Long(totalVotesCount).doubleValue();
            return 100 * (votes / totalVotes);
        }
    }
}
