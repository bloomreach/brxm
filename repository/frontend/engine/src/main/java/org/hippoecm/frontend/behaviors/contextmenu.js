/*
 * Copyright 2008 Hippo
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

(function() {

	if (typeof Hippo == 'undefined') {
		Hippo = new Object();
	}
	
	Hippo.Set = function() {
		this.entries = [];
	}
	
	Hippo.Set.prototype = {
		add : function (entry) {
			this.entries.push(entry);
		},
	
		remove : function (entry) {
			var index = this._getIndex(entry);
			if (index >= 0) {
			    var part1 = this.entries.slice(0, index);
			    var part2 = this.entries.slice(index + 1);
	
			    entries = part1.concat(part2);
			    return index;
			}
			return null;
		},

		contains : function (entry) {
			return this._getIndex(entry) >= 0;
		},
		
		_getIndex : function (entry) {
	        for (var i = 0; i < this.entries.length; i++) {
	            if (this.entries[ i ] == entry) {
	                return i;
	            }
	        }
	        return -1;
	    }
	}

	var menus = new Hippo.Set();

	Hippo.ContextMenu = new Object();
	
	Hippo.ContextMenu.show = function(id) {
		menus.add(id);
	}

	Hippo.ContextMenu.hide = function(id) {
		menus.remove(id);
	}

	Hippo.ContextMenu.isShown = function(id) {
		return menus.contains(id);
	}
	
})();
