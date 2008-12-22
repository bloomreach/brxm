/*
 *  Copyright 2008 Hippo.
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
if(!Hippo_scroll_registered) {
    var getEl = function() {
        var classname = 'hippo-tree';
        var node = document.getElementById('${id}');
        var re = new RegExp('\\b' + classname + '\\b');
        var els = node.getElementsByTagName("*");
        for(var i=0,j=els.length; i<j; i++) {
            if(re.test(els[i].className)) {
                return els[i];
            }
        }
        return null;
    }

    var func1 = function() {
		Hippo_scroll_savePosition(getEl());
	}
	Wicket.Ajax.registerPreCallHandler(func1);
	
	var func2 = function() {
		Hippo_scroll_loadPosition(getEl());
	}
	Wicket.Ajax.registerPostCallHandler(func2);
	Hippo_scroll_registered = true;
}
