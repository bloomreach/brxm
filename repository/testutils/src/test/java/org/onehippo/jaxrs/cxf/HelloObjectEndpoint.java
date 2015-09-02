/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.jaxrs.cxf;

import java.time.LocalDateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/helloobject")
public class HelloObjectEndpoint {

    public static class Date {
        private int year;
        private int month;
        private int day;

        public Date() {
        }
        public Date(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public int getYear() {
            return year;
        }
        public void setYear(int year) {
            this.year = year;
        }
        public int getMonth() {
            return month;
        }
        public void setMonth(int month) {
            this.month = month;
        }
        public int getDay() {
            return day;
        }
        public void setDay(int day) {
            this.day = day;
        }

        public static Date now() {
            LocalDateTime now = LocalDateTime.now();
            return new Date(now.getYear(), now.getMonth().getValue(), now.getDayOfMonth());
        }
    }

    public static class StructuredMessage {
        private String message;
        private Date timestamp;

        @SuppressWarnings("unused")
        public StructuredMessage() {
        }

        public StructuredMessage(String message) {
            this.message = message;
            this.timestamp = Date.now();
        }

        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
        public Date getTimestamp() {
            return timestamp;
        }
        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }

    @GET
    @Produces("application/json")
    public StructuredMessage doGet() {
        return new StructuredMessage("Hello object");
    }
}
