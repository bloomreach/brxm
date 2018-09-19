/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor
import javax.jcr.Node
import javax.jcr.RepositoryException
import javax.jcr.Session

import org.onehippo.cms7.services.HippoServiceRegistry
import org.onehippo.cms7.services.hst.Channel
import org.hippoecm.hst.configuration.channel.ChannelManagerEvent
import org.hippoecm.hst.core.request.HstRequestContext
import org.hippoecm.hst.platform.api.ChannelManagerEventBus
import org.hippoecm.hst.pagecomposer.jaxrs.api.BeforeChannelDeleteEvent
import org.hippoecm.hst.pagecomposer.jaxrs.api.BeforeChannelDeleteEventImpl
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEventImpl
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyContext
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyEvent
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyContextImpl
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyEventImpl
import org.hippoecm.hst.site.request.HstRequestContextImpl

/**
 * Simple boilerplating script to test how channel(manager)events are posted at runtime.
 */
class TestPostChannelManagerEventUpdater extends BaseNodeUpdateVisitor {

  def cmEventBus

  void initialize(Session session) {
    cmEventBus = HippoServiceRegistry.getService(ChannelManagerEventBus.class)
  }

  boolean doUpdate(Node node) {
    log.debug "Visiting node ${node.path}"

    def testEventType = parametersMap.get("testEventType")
    def targetContextPath = parametersMap.get("targetContextPath")

    def event = null

    if (testEventType == "PageCopyEvent") {
      event = createMockPageCopyContext()
    } else if (testEventType == "BeforeChannelDeleteEvent") {
      event = createMockBeforeChannelDeleteEvent()
    } else if (testEventType == "ChannelEvent") {
      event = createMockChannelEvent()
    } else if (testEventType == "ChannelManagerEvent") {
      event = createMockChannelManagerEvent()
    }

    if (event != null) {
      cmEventBus.post(event, targetContextPath)
    }

    return false
  }

  boolean undoUpdate(Node node) {
    throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
  }

  PageCopyEvent createMockPageCopyContext() {
    return new PageCopyEventImpl(
      new Channel(),
      new PageCopyContextImpl(
        new HstRequestContextImpl(null),
        null, null, null, null, null, null, null, null, null
      )
    )
  }

  BeforeChannelDeleteEvent createMockBeforeChannelDeleteEvent() {
    return new BeforeChannelDeleteEventImpl(
      new Channel(),
      new HstRequestContextImpl(null),
      new ArrayList()
    )
  }

  ChannelEvent createMockChannelEvent() {
    def event = new ChannelEventImpl(
      new Channel(),
      new HstRequestContextImpl(null),
      ChannelEvent.ChannelEventType.PUBLISH
    )

    return event
  }

  ChannelManagerEvent createMockChannelManagerEvent() {
    def event = [
      getChannelManagerEventType: {-> return ChannelManagerEvent.ChannelManagerEventType.CREATING },
      getBlueprint: {-> return null },
      getChannelId: {-> return null },
      getChannel: {-> return null },
      getConfigRootNode: {-> return null }
    ] as ChannelManagerEvent

    return event
  }
}
