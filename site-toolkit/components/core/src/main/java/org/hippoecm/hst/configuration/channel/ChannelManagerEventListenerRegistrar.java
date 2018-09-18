/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.channel;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.ChannelException.Type;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException.Status;
import org.hippoecm.hst.container.RequestContextProvider;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ProxiedServiceHolder;
import org.onehippo.cms7.services.ProxiedServiceTracker;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Responsible for registering and unregistering {@link ChannelManagerEventListener} beans
 * defined in an hst site web application, wrapping each listener by {@link SubscribingChannelManagerEventListenerDelegate}
 * to subscribe guava events.
 */
public class ChannelManagerEventListenerRegistrar {

    private static Logger log = LoggerFactory.getLogger(ChannelManagerEventListenerRegistrar.class);

    private List<ChannelManagerEventListener> channelManagerEventListeners;

    private ProxiedServiceTracker<ChannelManagerEventListenerRegistry> cmEventListenerRegistryTracker;

    private SubscribingChannelManagerEventListenerCollectionDelegate eventListenerDelegate;

    public void setChannelManagerEventListeners(List<ChannelManagerEventListener> channelManagerEventListeners) {
        this.channelManagerEventListeners = new ArrayList<>();

        if (channelManagerEventListeners != null) {
            this.channelManagerEventListeners.addAll(channelManagerEventListeners);
        }
    }

    public void init() {
        cmEventListenerRegistryTracker = new ProxiedServiceTracker<ChannelManagerEventListenerRegistry>() {
            @Override
            public void serviceRegistered(final ProxiedServiceHolder<ChannelManagerEventListenerRegistry> holder) {
                if (channelManagerEventListeners == null || channelManagerEventListeners.isEmpty()) {
                    return;
                }

                final ChannelManagerEventListenerRegistry registry = holder.getServiceProxy();
                eventListenerDelegate = new SubscribingChannelManagerEventListenerCollectionDelegate(
                        channelManagerEventListeners);
                registry.registerChannelManagerEventListener(eventListenerDelegate);
            }

            @Override
            public void serviceUnregistered(final ProxiedServiceHolder<ChannelManagerEventListenerRegistry> holder) {
                if (eventListenerDelegate != null) {
                    final ChannelManagerEventListenerRegistry registry = holder.getServiceProxy();
                    registry.unregisterChannelManagerEventListener(eventListenerDelegate);
                }
            }
        };

        HippoServiceRegistry.addTracker(cmEventListenerRegistryTracker, ChannelManagerEventListenerRegistry.class);
    }

    public void destroy() {
        HippoServiceRegistry.removeTracker(cmEventListenerRegistryTracker, ChannelManagerEventListenerRegistry.class);
    }

    /**
     * Delegating {@link ChannelManagerEventListener} implementation to delegate the event to all the internal
     * {@link ChannelManagerEventListener} collection, with annotating with Guava {@link Subscribe}.
     */
    public static class SubscribingChannelManagerEventListenerCollectionDelegate
            implements ChannelManagerEventListener {

        private final List<ChannelManagerEventListener> listeners;

        private SubscribingChannelManagerEventListenerCollectionDelegate(
                final List<ChannelManagerEventListener> listeners) {
            this.listeners = listeners;
        }

        @Subscribe
        public void onChannelManagerEvent(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
            switch (event.getChannelManagerEventType()) {
            case CREATED:
                channelCreated(event);
                break;
            case UPDATED:
                channelUpdated(event);
                break;
            default:
                log.warn("Unknown or unhandlable channel manager event type: {}", event);
                break;
            }
        }

        @Override
        public void channelCreated(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
            for (ChannelManagerEventListener listener : listeners) {
                try {
                    listener.channelCreated(event);
                } catch (ChannelManagerEventListenerException e) {
                    if (e.getStatus() == Status.STOP_CHANNEL_PROCESSING) {
                        refreshJcrSession(false);
                        log.info(
                                "Removing just created root content node at '{}' due ChannelManagerEventListenerException '{}'",
                                event.getChannel().getContentRoot(), e.toString());
                        removeTemporarilyCreatedContentRootNode(event.getChannel());
                        saveJcrSession();
                        throw new ChannelException(
                                "Channel creation stopped by listener '" + listener.getClass().getName() + "'", e,
                                Type.STOPPED_BY_LISTENER, e.getMessage());
                    } else {
                        log.warn("Channel created event listener, " + listener
                                + ", failed to handle the event. Continue channel processing", e);
                    }
                } catch (Exception listenerEx) {
                    log.warn("Channel created event listener, " + listener + ", failed to handle the event",
                            listenerEx);
                }
            }
        }

        @Override
        public void channelUpdated(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
            for (ChannelManagerEventListener listener : listeners) {
                try {
                    listener.channelUpdated(event);
                } catch (ChannelManagerEventListenerException e) {
                    if (e.getStatus() == Status.STOP_CHANNEL_PROCESSING) {
                        refreshJcrSession(false);
                        throw new ChannelException(
                                "Channel '" + event.getChannel().getId() + "' update stopped by listener '"
                                        + listener.getClass().getName() + "'",
                                e, Type.STOPPED_BY_LISTENER, e.getMessage());
                    } else {
                        log.warn("Channel created event listener, " + listener
                                + ", failed to handle the event. Continue channel processing", e);
                    }
                } catch (Exception listenerEx) {
                    log.error("Channel updated event listener, " + listener + ", failed to handle the event",
                            listenerEx);
                }
            }
        }

        @Override
        public String toString() {
            return super.toString() + "{internalListeners=" + listeners + "}";
        }

        private void refreshJcrSession(boolean keepChanges) {
            try {
                RequestContextProvider.get().getSession().refresh(keepChanges);
            } catch (RepositoryException e) {
                log.warn("Failed to refresh JCR session.", e);
            }
        }

        private void removeTemporarilyCreatedContentRootNode(final Channel channel) {
            final String contentRootPath = channel.getContentRoot();

            try {
                final Session jcrSession = RequestContextProvider.get().getSession();

                if (jcrSession.nodeExists(contentRootPath)) {
                    final Node contentRootNode = jcrSession.getNode(contentRootPath);
                    contentRootNode.remove();
                }
            } catch (RepositoryException e) {
                log.warn("Failed to remove the temporarily created content root node at {}.", contentRootPath, e);
            }
        }

        private void saveJcrSession() {
            try {
                RequestContextProvider.get().getSession().save();
            } catch (RepositoryException e) {
                log.warn("Failed to save JCR session.", e);
            }
        }
    }
}
