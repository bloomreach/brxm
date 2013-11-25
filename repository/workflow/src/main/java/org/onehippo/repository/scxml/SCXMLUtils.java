/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.scxml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.env.SimpleDispatcher;
import org.apache.commons.scxml2.env.Tracer;
import org.apache.commons.scxml2.env.jexl.JexlContext;
import org.apache.commons.scxml2.env.jexl.JexlEvaluator;
import org.apache.commons.scxml2.io.SCXMLReader;
import org.apache.commons.scxml2.io.SCXMLReader.Configuration;
import org.apache.commons.scxml2.model.CustomAction;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.SCXML;
import org.apache.commons.scxml2.model.TransitionTarget;

/**
 * SCXMLUtils
 */
public class SCXMLUtils {

    private SCXMLUtils() {
    }

    public static SCXML loadSCXML(URL url) throws IOException, ModelException, XMLStreamException {
        List<CustomAction> customActions = new LinkedList<CustomAction>();
        Configuration configuration = new Configuration(null, null, customActions);
        return loadSCXML(url, configuration);
    }

    public static SCXML loadSCXML(URL url, Configuration configuration) throws IOException, ModelException, XMLStreamException {
        SCXML scxml = null;

        InputStream is = null;
        BufferedInputStream bis = null;

        try {
            is = url.openStream();
            bis = new BufferedInputStream(is);
            scxml = loadSCXML(bis, configuration);
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
        }

        return scxml;
    }

    public static SCXML loadSCXML(InputStream is, Configuration configuration) throws IOException, ModelException, XMLStreamException {
        SCXML scxml = loadSCXML(new StreamSource(is), configuration);
        return scxml;
    }

    public static SCXML loadSCXML(Source source, Configuration configuration) throws IOException, ModelException, XMLStreamException {
        SCXML scxml = SCXMLReader.read(source, configuration);
        return scxml;
    }

    public static SCXMLExecutor createSCXMLExecutor(SCXML scxml) {
        return createSCXMLExecutor(scxml, new JexlContext());
    }

    public static SCXMLExecutor createSCXMLExecutor(SCXML scxml, Context rootCtx) {
        SCXMLExecutor executor = new SCXMLExecutor();
        executor.setEvaluator(new JexlEvaluator());
        executor.setEventdispatcher(new SimpleDispatcher());
        executor.setErrorReporter(new Tracer());
        executor.setStateMachine(scxml);
        executor.setRootContext(rootCtx);
        return executor;
    }

    public static List<TransitionTarget> getCurrentTransitionTargetList(SCXMLExecutor executor) {
        Set<TransitionTarget> targets = executor.getCurrentStatus().getStates();

        if (CollectionUtils.isEmpty(targets)) {
            return Collections.emptyList();
        }

        return new LinkedList<TransitionTarget>(targets);
    }

    public static List<String> getCurrentTransitionTargetIdList(SCXMLExecutor executor) {
        Set<TransitionTarget> targets = executor.getCurrentStatus().getStates();

        if (CollectionUtils.isEmpty(targets)) {
            return Collections.emptyList();
        }

        List<String> list = new LinkedList<String>();

        for (TransitionTarget target : targets) {
            list.add(target.getId());
        }

        return list;
    }

    public static void triggerSignalEvents(SCXMLExecutor executor, String ... actions) throws ModelException {
        TriggerEvent [] events = new TriggerEvent[actions.length];

        for (int i = 0; i < actions.length; i++) {
            events[i] = new TriggerEvent(actions[i], TriggerEvent.SIGNAL_EVENT);
        }

        executor.triggerEvents(events);
    }

    public static void triggerSignalEventWithPayload(SCXMLExecutor executor, String action, Object payload) throws ModelException {
        TriggerEvent event = new TriggerEvent(action, TriggerEvent.SIGNAL_EVENT, payload);
        executor.triggerEvent(event);
    }

    public static void triggerSignalEventsWithPayloads(SCXMLExecutor executor, String [] actions, Object [] payloads) throws ModelException {
        TriggerEvent [] events = new TriggerEvent[actions.length];

        for (int i = 0; i < actions.length; i++) {
            events[i] = new TriggerEvent(actions[i], TriggerEvent.SIGNAL_EVENT, payloads[i]);
        }

        executor.triggerEvents(events);
    }

}
