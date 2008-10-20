/*
Name: FancyTooltips
Version: 1.2.1
URI: http://www.victr.lm85.com/FancyTooltips/
Description: FancyTooltips creates dynamic tooltips from anchors (links), acronyms, inserts, deletions, and images.
Author: Victor Kulinski
Author URI: http://www.victr.lm85.com/

Current: Copyright (c) 2005 - Victor Kulinski

Re-released: Copyright (c) 2003 - Dunstan Orchard, Ethan Marcotte, Mark Wubben
Original: Copyright (c) 2003 - Stuart Langridge, Paul McLanahan, Peter Janes, Brad Choate

As originally released by Stuart Langridge, this script is licensed under MIT - http://www.opensource.org/licenses/mit-license.php

"THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."
*/

	// Display FancyTooltips in a specific ID.
	// - Disabled by default, to turn it on, enter the specified ID within ''.
	// - by Chris Beaven and Bretty Taylor - http://www.webfroot.co.nz/

var sOnlyInThisID = '';

function FancyTooltips(sTemplate, nDelay, nStringMaxLength, nMarginX, nMarginY, sContainerID, sClassName){
	var oTimer;
	var isActive = false;
	var sNameSpaceURI = "http://www.w3.org/1999/xhtml";
	
	if(!sTemplate){ sTemplate = "attr(fancytooltip)";}
	if(!nDelay || nDelay <= 0){ nDelay = false;}
	if(!nStringMaxLength){ nStringMaxLength = 8000; }
	if(!nMarginX){ nMarginX = 15; }
	if(!nMarginY){ nMarginY = 35; }
	if(!sContainerID){ sContainerID = "fancytooltipcontainer";}
	if(!sClassName){ sClassName = "fancytooltip";}

	var oContainer = document.getElementById(sContainerID);
	if(!oContainer){
		oContainer = document.createElementNS ? document.createElementNS(sNameSpaceURI, "div") : document.createElement("div");
		oContainer.setAttribute("id", sContainerID);
		oContainer.className = sClassName;
		oContainer.style.display = "none";
		document.getElementsByTagName("body").item(0).appendChild(oContainer);
	}
	
	// Method addElements (Public)
	// by Dunstan Orchard - http://www.1976design.com/
	
	this.addElements = function addElements(collNodes, sAttribute){
		var currentNode, sTitle;
		
		for(var i = 0; i < collNodes.length; i++){
			currentNode = collNodes[i];
		
			sTitle = currentNode.getAttribute(sAttribute);
			if(sTitle){
				currentNode.setAttribute("fancytooltip", sTitle);
				currentNode.removeAttribute(sAttribute);
				addEvent(currentNode, 'mouseover', show);
				addEvent(currentNode, 'mouseout', hide);
				addEvent(currentNode, 'focus', show);
				addEvent(currentNode, 'blur', hide);
			}
		}

	}
	
	// Other Methods (All Private)
	// by Dunstan Orchard - http://www.1976design.com/
	
	function show(e){
		if(isActive){ hide(); }

		var oNode = window.event ? window.event.srcElement : e.currentTarget;
		if(!oNode.getAttribute("fancytooltip")){ 
			while(oNode.parentNode){
				oNode = oNode.parentNode; // immediately goes to the parent, thus we can only have element nodes
				if(oNode.getAttribute("fancytooltip")){ break;	}
			}
		}

		var sOutput = parseTemplate(oNode);
		setContainerContent(sOutput);
		var oPosition = getPosition(e, oNode);
		oContainer.style.left = oPosition.x;
		oContainer.style.top = oPosition.y;

		if(nDelay){	
			oTimer = setTimeout(function(){oContainer.style.display = "block";}, nDelay);
		} else {
			oContainer.style.display = "block";
		}

		isActive = true;		
		// Let's put this event to a halt before it starts messing things up.
		window.event ? window.event.cancelBubble = true : e.stopPropagation();
	}
	
	function hide(){
		clearTimeout(oTimer);
		oContainer.style.display = "none";
		removeContainerContent();
		isActive = false;
	}

	function setContainerContent(sOutput){
		if(document.createElementNS && window.DOMParser){
			var oXMLDoc = (new DOMParser()).parseFromString("<root xmlns=\""+sNameSpaceURI+"\">"+sOutput+"</root>", "text/xml");
			var oOutputNode = document.importNode(oXMLDoc.documentElement, true);
			var oChild = oOutputNode.firstChild;
			var nextChild;
			while(oChild){
				nextChild = oChild.nextSibling; // One's the child is appended, the nextSibling reference is gone.
				oContainer.appendChild(oChild);
				oChild = nextChild;
			}
		} else {
			oContainer.innerHTML = sOutput;
		}
	}
	
	function removeContainerContent(){
		var oChild = oContainer.firstChild;
		var nextChild;

		if(!oChild){ return; }
		while(oChild){
			nextChild = oChild.nextSibling;
			oContainer.removeChild(oChild);
			oChild =  nextChild;
		}
	}
	
	function getPosition(e, oNode){
		var oViewport = getViewport();
		var oCoords;
		var commonEventInterface = window.event ? window.event : e;

		if(commonEventInterface.type == "focus"){
			oCoords = getNodePosition(oNode);	
			oCoords.x += nMarginX;
			oCoords.y += nMarginY;			
		} else {
			oCoords = { x : commonEventInterface.clientX + oViewport.x + nMarginX, y : commonEventInterface.clientY + oViewport.y + nMarginY};
		}

		oContainer.style.visiblity = "hidden"; // oContainer needs to be displayed before width and height can be retrieved.
		oContainer.style.display =  "block";
		var containerWidth = oContainer.offsetWidth;
		var containerHeight = oContainer.offsetHeight;
		oContainer.style.display = "none"; // And hide it again.
		oContainer.style.visiblity = "visible";

		if(oCoords.x + containerWidth + 10 >= oViewport.width + oViewport.x){
			oCoords.x = oViewport.width + oViewport.x - containerWidth - 10;
		}
		if(oCoords.y + containerHeight + 10 >= oViewport.height + oViewport.y){
			oCoords.y = oViewport.height + oViewport.y - containerHeight - oNode.offsetHeight - 10;
		}

		oCoords.x += "px";
		oCoords.y += "px";

		return oCoords;
	}

	function parseTemplate(oNode){
		var sAttribute, collOptionalAttributes;
		var oFound = {};
		var sResult = sTemplate;
		
		// handle content() template
		if(sResult.match(/content\(\)/)){
			sResult = sResult.replace(/content\(\)/g, getContentOfNode(oNode));
		}
		
		// find attr() templates and fetch values
		var collSearch = sResult.split(/attr\(/);
		for(var i = 1; i < collSearch.length; i++){
			sAttribute = collSearch[i].split(")")[0];
			oFound[sAttribute] = oNode.getAttribute(sAttribute);
			if(oFound[sAttribute] && oFound[sAttribute].length > nStringMaxLength){
				oFound[sAttribute] = oFound[sAttribute].substring(0, nStringMaxLength) + "...";
			}
		}
		
		// remove optional attr() templates if values are not found
		var collOptional = sResult.split("?")
		for(var i = 1; i < collOptional.length; i += 2){
			collOptionalAttributes = collOptional[i].split("attr(");
			for(var j = 1; j < collOptionalAttributes.length; j++){
				sAttribute = collOptionalAttributes[j].split(")")[0];

				if(!oFound[sAttribute]){ sResult = sResult.replace(new RegExp("\\?[^\\?]*attr\\("+sAttribute+"\\)[^\\?]*\\?", "g"), "");	}
			}
		}
		sResult = sResult.replace(/\?/g, "");
		
		// replace attr() templates
		for(sAttribute in oFound){
			// html escape output
			var sOutput = oFound[sAttribute];
			if (sOutput) {
				sOutput = sOutput.replace(/&/g, "&amp;");
				sOutput = sOutput.replace(/</g, "&lt;");
				sOutput = sOutput.replace(/>/g, "&gt;");
				sOutput = sOutput.replace(/\n/g, "<br />");
			}
			sResult = sResult.replace("attr\("+sAttribute+"\)", sOutput);
		}
		
		return sResult;
	}	
		
	function getContentOfNode(oNode){
		var sContent = "";
		var oSearch = oNode.firstChild;

		while(oSearch){
			if(oSearch.nodeType == 3){
				sContent += oSearch.nodeValue;
			} else if(oSearch.nodeType == 1 && oSearch.hasChildNodes){
				sContent += getContentOfNode(oSearch);
			}
			oSearch = oSearch.nextSibling
		}

		return sContent;
	}
	
	function getNodePosition(oNode){
		var x = 0;
		var y = 0;

		do {
			if(oNode.offsetLeft){ x += oNode.offsetLeft }
			if(oNode.offsetTop){ y += oNode.offsetTop }
		}	while((oNode = oNode.offsetParent) && !document.all) // IE gets the offset 'right' from the start

		return {x : x, y : y}
	}
	
	// Idea from 13thParallel: http://13thparallel.net/?issue=2002.06&title=viewport.
	
	function getViewport(){
		var width = 0;
		var height = 0;
		var x = 0;
		var y = 0;
		
		if(document.documentElement && document.documentElement.clientWidth){
			width = document.documentElement.clientWidth;
			height = document.documentElement.clientHeight;
			x = document.documentElement.scrollLeft;
			y = document.documentElement.scrollTop;
		} else if(document.body && document.body.clientWidth){
			width = document.body.clientWidth;
			height = document.body.clientHeight;
			x = document.body.scrollLeft;
			y = document.body.scrollTop;
		}
		// we don't use an else if here, since Opera 7 tends to get the height on the documentElement wrong
		if(window.innerWidth){ 
			width = window.innerWidth - 18;
			height = window.innerHeight - 18;
		}
		
		if(window.pageXOffset){
			x = window.pageXOffset;
			y = window.pageYOffset;
		} else if(window.scrollX){
			x = window.scrollX;
			y = window.scrollY;
		}
		
		return {width : width, height : height, x : x, y : y };		
	}
}

// Event Listener
// by Scott Andrew - http://scottandrew.com
// edited by Mark Wubben, <useCapture> is now set to false

function addEvent(obj, evType, fn){
	if(obj.addEventListener){
		obj.addEventListener(evType, fn, false); 
		return true;
	} else if (obj.attachEvent){
		var r = obj.attachEvent('on'+evType, fn);
		return r;
	} else {
		return false;
	}
}

// Time Since
// by Mark Wubben - http://neo.dzygn.com

Date.prototype.toTimeSinceString = function(nLimit, sBetween, sLastBetween){
	if(!nLimit){ nLimit = 2; }
	if(!sBetween){ sBetween = ", "; }
	if(!sLastBetween){ sLastBetween = " and "; }
	if(!Date.prototype.toTimeSinceString._collStructs){
		Date.prototype.toTimeSinceString._collStructs = new Array(
			{seconds: 60 * 60 * 24 * 365, name: "year"},
			{seconds: 60 * 60 * 24 * 30, name: "month"},
			{seconds: 60 * 60 * 24 * 7, name: "week"},
			{seconds: 60 * 60 * 24, name: "day"},
			{seconds: 60 * 60, name: "hour"},
			{seconds: 60, name: "minute"}
		);
	}

	var collStructs = Date.prototype.toTimeSinceString._collStructs;
	var nSecondsRemain = ((new Date).valueOf() - this.valueOf()) / 1000;
	var sReturn = "";
	var nCount = 0;
	var nFloored;

	for(var i = 0; i < collStructs.length && nCount < nLimit; i++){
		nFloored = Math.floor(nSecondsRemain / collStructs[i].seconds);
		if(nFloored > 0){
			if(sReturn.length > 0){
				if(nCount == nLimit - 1 || i == collStructs.length - 1){
					sReturn += sLastBetween;
				} else if(nCount < nLimit && i < collStructs.length){
					sReturn += sBetween;
				}
			}
			sReturn += nFloored + " " + collStructs[i].name;
			if(nFloored > 1){
				sReturn += "s";
			}
			nSecondsRemain -= nFloored * collStructs[i].seconds;
			nCount++;
		}
	}

	return sReturn;
}


// Here the default nice titles are created
// by Dunstan Orchard - http://www.1976design.com/
// Improved by Victor Kulinski - http://www.victr.lm85.com/

FancyTooltips.autoCreation = function(){
	if(!document.getElementsByTagName){ return; }

	function rewriteDateTime(collNodes){
		var nMonth, nDay, nHours, nMinutes, nSeconds, sDateTime, oDate;
		for(var i = 0; i < collNodes.length; i++){
			sDateTime = collNodes[i].getAttribute("datetime");
			if(sDateTime != null || sDateTime != ""){
				nYear = Number(sDateTime.substring(0,4));
				nMonth = Number(sDateTime.substring(5,7)) - 1;
				nDay = Number(sDateTime.substring(8,10));
				nHours = Number(sDateTime.substring(11, 13));
				nMinutes = Number(sDateTime.substring(14,16));
				nSeconds = Number(sDateTime.substring(17,19));
				oDate = new Date(nYear, nMonth, nDay, nHours, nMinutes, nSeconds);
				collNodes[i].setAttribute("nicetime", oDate.toTimeSinceString());
				collNodes[i].setAttribute("gmttime", oDate.toGMTString());
			}
		}

		return collNodes;
	}

	FancyTooltips.autoCreated = new Object();

	FancyTooltips.autoCreated.anchors = new FancyTooltips("<p class=\"titletext\">attr(fancytooltip)? <span class=\"accesskey\">[attr(accesskey)]</span>?</p><p class=\"destination\">attr(href)</p>", 600);
	FancyTooltips.autoCreated.inserts = new FancyTooltips("<p class=\"titletext\">Added attr(fancytooltip) ago</p><p class=\"destination\">Complete timestamp: attr(gmttime)</p>?<p class=\"destination\">Reason: attr(cite)</p>?", 600);
	FancyTooltips.autoCreated.deletions = new FancyTooltips("<p class=\"titletext\">Deleted attr(fancytooltip) ago</p><p class=\"destination\">Complete timestamp: attr(gmttime)</p>?<p class=\"destination\">Reason: attr(cite)</p>?", 600);
	FancyTooltips.autoCreated.acronyms = new FancyTooltips("<p class=\"titletext\">content(): attr(fancytooltip)</p>", 600);	
	FancyTooltips.autoCreated.abbreviations = new FancyTooltips("<p class=\"titletext\">content(): attr(fancytooltip)</p>", 600);	
	FancyTooltips.autoCreated.images = new FancyTooltips("<p class=\"titletext\">Caption: attr(fancytooltip)</p>", 600);


	// More of Restrict to ID
	// - by Chris Beaven and Brett Taylor - http://www.webfroot.co.nz/
	
	if (sOnlyInThisID) {
		oNode = document.getElementById(sOnlyInThisID);
	} else {
		oNode = null;
	}
	if (!oNode) {
		oNode = document;
	}
	
	FancyTooltips.autoCreated.anchors.addElements(oNode.getElementsByTagName("a"), "title");
	FancyTooltips.autoCreated.inserts.addElements(rewriteDateTime(oNode.getElementsByTagName("ins")), "nicetime");
	FancyTooltips.autoCreated.deletions.addElements(rewriteDateTime(oNode.getElementsByTagName("del")), "nicetime");
	FancyTooltips.autoCreated.acronyms.addElements(oNode.getElementsByTagName("acronym"), "title");
	FancyTooltips.autoCreated.acronyms.addElements(oNode.getElementsByTagName("abbr"), "title");
	
	// FancyTooltips img recognition.
	// - Turned off by default. To turn it on, remove /* */ around the
	// first line. This will display alt FancyTooltips. For title tooltips,
	// remove /* */ from the second line. This may, however, cause problems
	// in MSIE (Microsoft Internet Explorer.
	// - by Victor Kulinski - http://www.victr.lm85.com/
	
	/* FancyTooltips.autoCreated.images.addElements(oNode.getElementsByTagName("img"), "alt"); */
	/* FancyTooltips.autoCreated.images.addElements(document.getElementsByTagName("img"), "title"); */
	
}

addEvent(window, "load", FancyTooltips.autoCreation);
