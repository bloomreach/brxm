/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

var _gaq = _gaq || [];
if (typeof(Hippo_Ga_AccountId) != 'undefined' && Hippo_Ga_AccountId.length > 0) {
	_gaq.push( [ '_setAccount', Hippo_Ga_AccountId ]);	
}
if (typeof(Hippo_Ga_Documents) != 'undefined' && Hippo_Ga_Documents.length > 0) {
    for (var i = 0; i < Hippo_Ga_Documents.length; i++) {
        _gaq.push( [ '_trackPageview', Hippo_Ga_Documents[i] ]);
    }
}
else {
    _gaq.push( [ '_trackPageview' ]);
}
if (typeof(Hippo_Ga_CustomVars) != 'undefined' && Hippo_Ga_CustomVars.length > 0) {
	for (var i = 0; i < Hippo_Ga_CustomVars.length; i++) {
		var index = Hippo_Ga_CustomVars.index;
		var name = Hippo_Ga_CustomVars.name;
		var value = Hippo_Ga_CustomVars.value;
		var scope = Hippo_Ga_CustomVars.scope;
		_gaq.push(['_setCustomVar', index, name, value, scope ]);
	}
}
(function() {
    var ga = document.createElement('script');
    ga.type = 'text/javascript';
    ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl'
            : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(ga, s);
})();
