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
