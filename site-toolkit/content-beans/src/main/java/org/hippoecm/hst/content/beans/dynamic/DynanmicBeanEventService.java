/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;


import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO: Listening the JCR events should be replaced by content type service 
public class DynanmicBeanEventService implements EventListener{
	
	private static final Logger logger = LoggerFactory.getLogger(DynanmicBeanEventService.class);

	protected ObjectConverter objectConverter = null;
	
	public ObjectConverter getObjectConverter(){
		return objectConverter;
	}

	public void setObjectConverter(ObjectConverter objectConverter){
		this.objectConverter = objectConverter;
	}

	public void init() {
		
	}
	
	public void onEvent(EventIterator events) {
		try {
			while(events.hasNext()) {
				Event event = events.nextEvent();
				if(event.getPath().startsWith("/hippo:namespace")) {
					logger.info("Handling doctype change event -"+event.getPath());
					ObjectConverterUtils.flushDocTypeDynamicBean(event.getPath(),getObjectConverter());

					return;
				}
			}
		}catch(Exception e) {
			logger.error("error searching namespace events",e);
		}
		
	}

}
