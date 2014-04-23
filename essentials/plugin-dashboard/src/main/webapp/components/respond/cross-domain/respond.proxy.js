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

/*! Respond.js: min/max-width media query polyfill. Remote proxy (c) Scott Jehl. MIT/GPLv2 Lic. j.mp/respondjs  */
(function(win, doc, undefined){
	var docElem			= doc.documentElement,
		proxyURL		= doc.getElementById("respond-proxy").href,
		redirectURL		= (doc.getElementById("respond-redirect") || location).href,
		baseElem		= doc.getElementsByTagName("base")[0],
		urls			= [],
		refNode;

	function encode(url){
		return win.encodeURIComponent(url);
	}

	 function fakejax( url, callback ){

		var iframe,
			AXO;
		
		// All hail Google http://j.mp/iKMI19
		// Behold, an iframe proxy without annoying clicky noises.
		if ( "ActiveXObject" in win ) {
			AXO = new ActiveXObject( "htmlfile" );
			AXO.open();
			AXO.write( '<iframe id="x"></iframe>' );
			AXO.close();
			iframe = AXO.getElementById( "x" );
		} else {
			iframe = doc.createElement( "iframe" );
			iframe.style.cssText = "position:absolute;top:-99em";
			docElem.insertBefore(iframe, docElem.firstElementChild || docElem.firstChild );
		}

		iframe.src = checkBaseURL(proxyURL) + "?url=" + encode(redirectURL) + "&css=" + encode(checkBaseURL(url));
		
		function checkFrameName() {
			var cssText;

			try {
				cssText = iframe.contentWindow.name;
			}
			catch (e) { }

			if (cssText) {
				// We've got what we need. Stop the iframe from loading further content.
				iframe.src = "about:blank";
				iframe.parentNode.removeChild(iframe);
				iframe = null;

			
				// Per http://j.mp/kn9EPh, not taking any chances. Flushing the ActiveXObject
				if (AXO) {
					AXO = null;

					if (win.CollectGarbage) {
						win.CollectGarbage();
					}
				}

				callback(cssText);
			}
			else{
				win.setTimeout(checkFrameName, 100);
			}
		}
		
		win.setTimeout(checkFrameName, 500);
	}

	function checkBaseURL(href) {
		if (baseElem && href.indexOf(baseElem.href) === -1) {
			bref = (/\/$/).test(baseElem.href) ? baseElem.href : (baseElem.href + "/");
			href = bref + href;
		}

		return href;
	}
	
	function checkRedirectURL() {
		// IE6 & IE7 don't build out absolute urls in <link /> attributes.
		// So respond.proxy.gif remains relative instead of http://example.com/respond.proxy.gif.
		// This trickery resolves that issue.
		if (~ !redirectURL.indexOf(location.host)) {

			var fakeLink = doc.createElement("div");

			fakeLink.innerHTML = '<a href="' + redirectURL + '"></a>';
			docElem.insertBefore(fakeLink, docElem.firstElementChild || docElem.firstChild );

			// Grab the parsed URL from that dummy object
			redirectURL = fakeLink.firstChild.href;

			// Clean up
			fakeLink.parentNode.removeChild(fakeLink);
			fakeLink = null;
		}
	}
	
	function buildUrls(){
		var links = doc.getElementsByTagName( "link" );
		
		for( var i = 0, linkl = links.length; i < linkl; i++ ){
			
			var thislink	= links[i],
				href		= links[i].href,
				extreg		= (/^([a-zA-Z:]*\/\/(www\.)?)/).test( href ),
				ext			= (baseElem && !extreg) || extreg;

			//make sure it's an external stylesheet
			if( thislink.rel.indexOf( "stylesheet" ) >= 0 && ext ){
				(function( link ){			
					fakejax( href, function( css ){
						link.styleSheet.rawCssText = css;
						respond.update();
					} );
				})( thislink );
			}	
		}

		
	}
	
	if( !respond.mediaQueriesSupported ){
		checkRedirectURL();
		buildUrls();
	}

})( window, document );
