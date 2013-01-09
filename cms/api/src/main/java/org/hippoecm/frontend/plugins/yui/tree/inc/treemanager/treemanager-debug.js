/*
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
 * Todo
 * </p>
 * @namespace YAHOO.hippo
 * @requires hippotree, functionqueue, hashmap
 * @module treemanager
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.TreeManager) {
    ( function() {
        YAHOO.hippo.TreeManagerImpl = function() {
        };
        
        YAHOO.hippo.TreeManagerImpl.prototype = {

			loader: new YAHOO.hippo.FunctionQueue('TreeQueue'),
			instances : new YAHOO.hippo.HashMap(),
			
			onLoad: function() {
			    this.cleanup();
				this.loader.handleQueue();
			},
			
			cleanup: function() {
				//remove old autoc components from dom and maps
			},
			
			load: function(id, clazz, config) {
				this._add(id, clazz, config, this.instances);
			},

			_add: function(id, Clazz, config, map) {
				var func = function() {
					var c = new Clazz(id, config);
					map.put(id, c);
					c.render();
				};
				this.loader.registerFunction(func);
			}
			
        };
    }());

    YAHOO.hippo.TreeManager = new YAHOO.hippo.TreeManagerImpl();
    YAHOO.register("treemanager", YAHOO.hippo.TreeManager, {
        version :"2.6.0",
        build :"1321"
    });
}