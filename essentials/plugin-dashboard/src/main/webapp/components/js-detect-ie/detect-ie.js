/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var IE = (function () {
	"use strict";

	var ret, isTheBrowser,
		actualVersion, currentVersion,
		jscriptMap, jscriptVersion;

	isTheBrowser = false;
	jscriptMap = {
		"5.5": "5.5",
		"5.6": "6",
		"5.7": "7",
		"5.8": "8",
		"9": "9",
		"10": "10"
	};
	jscriptVersion = new Function("/*@cc_on return @_jscript_version; @*/")();

	if (typeof jscriptVersion !== "undefined") {
		isTheBrowser = true;
		actualVersion = jscriptMap[jscriptVersion];
		currentVersion = "" + document.documentMode;
	}

	ret = {
		isTheBrowser: isTheBrowser,
		actualVersion: actualVersion,
		currentVersion: currentVersion
	};

	return ret;
}());
