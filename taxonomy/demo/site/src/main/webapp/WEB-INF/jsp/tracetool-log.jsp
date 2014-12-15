<%--
  Copyright 2008 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%><%@ page language="java" import="java.util.*, java.text.*, org.slf4j.helpers.MessageFormatter, org.hippoecm.hst.logging.*, org.hippoecm.hst.site.HstServices" %><%
LogEventBuffer traceLogEventBuffer = (LogEventBuffer) HstServices.getComponentManager().getComponent("hstTraceToolLogEventBuffer");
List logEventList = new LinkedList();
synchronized (traceLogEventBuffer) {
    for (Iterator it = traceLogEventBuffer.iterator(); it.hasNext(); ) {
        logEventList.add(it.next());
    }
}
DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
for (Iterator it = logEventList.iterator(); it.hasNext(); ) {
    LogEvent logEvent = (LogEvent) it.next();
    Date ts = new Date(logEvent.getTimestamp());
    String level = logEvent.getLevel().toString();
    String threadName = logEvent.getThreadName();
    Object [] args = new Object [] { df.format(ts), level, threadName, logEvent.getMessage() };
    out.println(MessageFormatter.arrayFormat("{} {} {} {}", args));
}
%>