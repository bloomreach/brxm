/*
 * Copyright 2009 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @description
 * <p>
 * Provides a singleton manager for feedback panels
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, event, container, hashmap
 * @module feedbackmanager
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.FeedbackManager) {
	(function() {
		var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

		YAHOO.hippo.FeedbackManagerImpl = function() {
		};

		YAHOO.hippo.FeedbackManagerImpl.prototype = {
			instances : new YAHOO.hippo.HashMap(),

			create : function(id, config) {
				YAHOO.log("Creating feedback panel [" + id + "]", "info", "FeedbackManager");
				this._add(id, config, this.instances);
			},

			get : function(id) {
				YAHOO.log("Retrieving feedback panel [" + id + "]", "info", "FeedbackManager");
				return this.instances.get(id);
			},

			_add : function(id, config, map) {
				var module = new YAHOO.widget.Module(id, config);
				map.put(id, module);

				module.render(document.body);
				YAHOO.util.Event.addListener(id, "click", module.hide, module, true);
			},

			_cleanup : function() {
				var ids = map.keySet();
				var toRemove = [];
				for (var i = 0; i < ids.length; i++) {
					var id = ids[i];
					if (Dom.get(id) == null) {
						toRemove.push(id);
					}
				}
				for (var i = 0; i < toRemove.length; i++) {
					map.remove(toRemove[i]);
				}
			}
		};

	})();

	YAHOO.hippo.FeedbackManager = new YAHOO.hippo.FeedbackManagerImpl();
	YAHOO.register("feedbackmanager", YAHOO.hippo.FeedbackManager, {
	    version: "2.7.0", build: "1799"
	});
}