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
var Hippo_scroll_registered = false;
var Hippo_scroll_element_id = null;

function Hippo_scroll_setTreeId(id) {
    Hippo_scroll_element_id = id;
}

function Hippo_scroll_savePosition() {
    var el = Hippo_scroll_getElement();
    if(el == null)
        return;
	var offsetY = el.pageYOffset || el.scrollTop;
	Hippo_scroll_setCookie('Hippo_scroll_offsetY', offsetY);
}

function Hippo_scroll_getElement() {
    var classname = 'hippo-tree';
    var elId = Hippo_scroll_element_id;
    var node = document.getElementById(elId);
    if(node == null) 
        return null;
    var re = new RegExp('\\b' + classname + '\\b');
    var els = node.getElementsByTagName("*");
    for(var i=0,j=els.length; i<j; i++) {
        if(re.test(els[i].className)) {
            return els[i];
        }
    }
    return null;
}


function Hippo_scroll_loadPosition() {
    var el = Hippo_scroll_getElement();
    if(el == null)
        return;
	var y = Hippo_scroll_getCookie('Hippo_scroll_offsetY');
	if (y) {
		el.scrollTop = y;
		Hippo_scroll_deleteCookie('Hippo_scroll_offsetY');
	}
}

function Hippo_scroll_setCookie(name, value, expires, path, domain, secure) {
	var curCookie = name + "=" + escape(value)
			+ ((expires) ? "; expires=" + expires.toGMTString() : "")
			+ ((path) ? "; path=" + path : "")
			+ ((domain) ? "; domain=" + domain : "")
			+ ((secure) ? "; secure" : "");
	document.cookie = curCookie;
}

function Hippo_scroll_getCookie(name) {
	var dc = document.cookie;
	var prefix = name + "=";
	var begin = dc.indexOf("; " + prefix);
	if (begin == -1) {
		begin = dc.indexOf(prefix);
		if (begin != 0) {
			return null;
		}
	} else {
		begin += 2;
	}
	var end = document.cookie.indexOf(";", begin);
	if (end == -1) {
		end = dc.length;
	}
	return unescape(dc.substring(begin + prefix.length, end));
}

function Hippo_scroll_deleteCookie(name, path, domain) {
	if (Hippo_scroll_getCookie(name)) {
		document.cookie = name + "=" + ((path) ? "; path=" + path : "")
				+ ((domain) ? "; domain=" + domain : "")
				+ "; expires=Thu, 01-Jan-70 00:00:01 GMT";
	}
}
