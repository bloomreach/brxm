/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

/*
 * This code borrows heavily from the following YUI examples:
 * 
 * http://developer.yahoo.com/yui/examples/editor/multi_editor.html
 * http://developer.yahoo.com/yui/examples/connection/post.html
 * 
 */

function initEditor(editableElementsContainerName, editorFormName, richEditorTextAreaName, plainEditorTextAreaName, toolbarContainerName, cmsAppUrl) {
    var Dom = YAHOO.util.Dom,
        Event = YAHOO.util.Event,
        editing = null;
    
    // Setup a stripped down config for the editor
    var myConfig = {
        height: '200px',
        width: '400px',
        focusAtStart: true,
        limitCommands: true,
        toolbar: {
            buttons: [
                { group: 'textstyle', label: ' ',
                    buttons: [
                        { type: 'push', label: 'Bold', value: 'bold' },
                        { type: 'push', label: 'Italic', value: 'italic' },
                        { type: 'push', label: 'Underline', value: 'underline' }
                        ]},
                { type: 'separator' },
                { group: 'fileactions', label: ' ',
                	buttons: [
                        { type: 'push', label: 'Save and close', value: 'save' },
                        { type: 'push', label: 'Close without saving', value: 'close' },
                        { type: 'push', label: 'Save and request publication', value: 'requestPublication' },
                        { type: 'push', label: 'Edit in CMS', value: 'editInCMS' }
                    ]
                }
            ]
        }
    };
    
    // Create a panel that masks the background to create a dim effect when the editor is open
    darkmask = new YAHOO.widget.Panel("darkmask",{
        xy: [-99999,-99999],
        close: false,
        draggable: false,
        zindex: 998,
        modal: true,
        visible: false});
    
    // Create the feedback panel
    feedbackPanel = new YAHOO.widget.Overlay("feedbackPanel", {
    	height: "50px",
    	width: "150px",
    	fixedcenter: true,
    	visible: false});
    feedbackPanel.setBody("");

    // Create the Editor..
    myEditor = new YAHOO.widget.Editor(richEditorTextAreaName, myConfig);
    myEditor.on('toolbarLoaded', function() {
    	
    	this.toolbar.on('saveClick', function(o) { 
            var formObject = document.getElementById(editorFormName); 
            formObject.workflowAction.value = "save";
            myEditor.saveHTML();
            save(editorFormName);
            editing.innerHTML = myEditor.get('element').value;
            close_editor();
    	}, myEditor, true);

    	this.toolbar.on('closeClick', function(o) { 
            var formObject = document.getElementById(editorFormName); 
            formObject.workflowAction.value = "close";
            close_editor();
    	}, myEditor, true);

    	this.toolbar.on('requestPublicationClick', function(o) { 
            // set the workflow action to perform (request publication)
            var formObject = document.getElementById(editorFormName); 
            formObject.workflowAction.value = "requestPublication";
        	
            myEditor.saveHTML();
            save(editorFormName);
            editing.innerHTML = myEditor.get('element').value;
            close_editor();
        }, myEditor, true);
    	
    	this.toolbar.on('editInCMSClick', function(o) {
            var formObject = document.getElementById(editorFormName); 
            formObject.workflowAction.value = "close";
    		// FIXME make CMS URL configurable
            window.open(cmsAppUrl, 'cms');
            close_editor();
        }, myEditor, true);
    	
        // Render the darkmask and the feedback panel
        darkmask.render(document.body);
    	feedbackPanel.render(document.body);
    	
    }, myEditor, true);
    
    myEditor.on('editorContentLoaded', function() {
        resize = new YAHOO.util.Resize(myEditor.get('element_cont').get('element'), {
            handles: ['br'],
            autoRatio: true,
            proxy: true,
            setSize: false
        });
        resize.on('startResize', function() {
            myEditor.hide();
            myEditor.set('disabled', true);
        }, myEditor, true);
        resize.on('resize', function(args) {
            var h = args.height;
            var th = (myEditor.toolbar.get('element').clientHeight + 2); //It has a 1px border..
            var newH = (h - th);
            myEditor.set('width', args.width + 'px');
            myEditor.set('height', newH + 'px');
            myEditor.set('disabled', false);
            myEditor.show();
        }, myEditor, true);
    });
    myEditor.render();
    
    Event.on(editableElementsContainerName, 'dblclick', function(ev) {
        var tar = Event.getTarget(ev);
        while(tar.id != editableElementsContainerName) {
            if (Dom.hasClass(tar, 'editable')) {
                // Set the Editors HTML with the elements innerHTML
                myEditor.setEditorHTML(tar.innerHTML);

                // set the document property that is being edited
                var formObject = document.getElementById(editorFormName);
                var tarId = tar.id;
                var seperatorIndex = tarId.indexOf("::");
                if( seperatorIndex >= 0) {
	                formObject.customnodepath.value = tarId.substring(seperatorIndex + 2);
	                formObject.field.value = tarId.substring(0, seperatorIndex);
                } else {
	                formObject.customnodepath.value = "";
	                formObject.field.value = tar.id;
                }
                
                
                // Get the height & width of the element that was clicked
                if (tar.currentStyle) {
                	// IE
                    var h = tar.currentStyle['height'];
            		var w = tar.currentStyle['width'];
                } else if (window.getComputedStyle) {
            		// Firefox, Opera, etc.
            		var h = document.defaultView.getComputedStyle(tar,null).getPropertyValue('height');
            		var w = document.defaultView.getComputedStyle(tar,null).getPropertyValue('width');
                }
                if (h == 'auto' || h == ''){
                   h='400';
                }
                if (w == 'auto' || w == ''){
                   w='458';
                }
                // Set the editor's height & width, minimum of 100/200 pixels
                try {
                	h.replace(/\D+/, "");
                	h = parseInt(h);
                	// Set only if height minus height of the toolbar is greater then 100 pixels
                	h = h - myEditor.toolbar.get('element').clientHeight + 2;
                	if(h >= 100)
                		myEditor.set("height",h + "px");
                	else
                		throw "";
                } catch (e) {
           			myEditor.set("height","100px");
                }
                
               	try {
               		w.replace(/\D+/, "");
                	w = parseInt(w);
               		if(w >= 200)
                		myEditor.set("width",w + "px");
                	else
                    	throw "";
               	} catch (e) {
           			myEditor.set("width","200px");
               	}
               	
               	// Reset the feedback panel
               	feedbackPanel.hide();
               	Dom.setStyle("feedbackPanel", "opacity", "1");
               	
               	// Make the original text invisible and show the editor
                Dom.setStyle(tar, "visibility", "hidden");
                Dom.setXY(myEditor.get('element_cont').get('element'), Dom.getXY(tar));
                
                // Fade out transition
                darkmask.show();
                var darkmaskOutAnim = new YAHOO.util.Anim("darkmask_mask", { opacity: {to: 0.5} }, 1 );
        		darkmaskOutAnim.animate();
                
                editing = tar;
            }
            else if (Dom.hasClass(tar, 'editable_plaintext')) {
            	
            	var plainTextEditor = document.getElementById(toolbarContainerName);
            	var value = document.getElementById(plainEditorTextAreaName);

            	value.value = tar.innerHTML;
            	
            	Dom.setStyle(value, 'color', Dom.getStyle(tar, 'color'));
            	Dom.setStyle(value, 'background-color', Dom.getStyle(tar, 'background-color'));
            	Dom.setStyle(value, 'font-size', Dom.getStyle(tar, 'font-size'));
            	Dom.setStyle(value, 'font-family', Dom.getStyle(tar, 'font-family'));
            	Dom.setStyle(value, 'font-weight', Dom.getStyle(tar, 'font-weight'));

                swapPosition(plainTextEditor, tar);

                var formObject = document.getElementById(editorFormName);
                formObject.field.value = tar.id;

            }
            tar = tar.parentNode;
        }
    });
    
    function swapPosition(element1, element2) {
        // Just switching the elements makes the text appear in the previous position of the editor, when switching between editable texts.
        // Therefore, move the element away from the screen, so the text doesn't get mixed up with other content.
        // var xy1 = Dom.getXY(element1);
        var xy1 = [-99999, -99999];
        var xy2 = Dom.getXY(element2);
        Dom.setXY(element1, xy2);
        Dom.setXY(element2, xy1);
    }
    
    Event.on(toolbarContainerName + '_save', 'click', function(ev) {
    	var plainTextEditor = document.getElementById(toolbarContainerName);
    	var value = document.getElementById(plainEditorTextAreaName);
        var formObject = document.getElementById(editorFormName);
        formObject.editor.value = value.value;
        formObject.workflowAction.value = "save";
        var tar = document.getElementById(formObject.field.value);
        tar.innerHTML = value.value;
        swapPosition(plainTextEditor, tar);
        save(editorFormName);
    });

    Event.on(toolbarContainerName + '_close', 'click', function(ev) {
    	var plainTextEditor = document.getElementById(toolbarContainerName);
    	var value = document.getElementById(plainEditorTextAreaName);
        var formObject = document.getElementById(editorFormName);
        formObject.workflowAction.value = "close";
        value.value = "";
        var tar = document.getElementById(formObject.field.value);
        swapPosition(plainTextEditor, tar);
    });

    Event.on(toolbarContainerName + '_requestPublication', 'click', function(ev) {
    	var plainTextEditor = document.getElementById(toolbarContainerName);
    	var value = document.getElementById(plainEditorTextAreaName);
        var formObject = document.getElementById(editorFormName);
        formObject.editor.value = value.value;
        formObject.workflowAction.value = "requestPublication";
        var tar = document.getElementById(formObject.field.value);
        tar.innerHTML = value.value;
        swapPosition(plainTextEditor, tar);
        save(editorFormName);
    });

    Event.on(toolbarContainerName + '_editInCMS', 'click', function(ev) {
		// FIXME make CMS URL configurable
        window.open('http://ecm', 'cms');
    	var plainTextEditor = document.getElementById(toolbarContainerName);
    	var value = document.getElementById(plainEditorTextAreaName);
        var formObject = document.getElementById(editorFormName);
        formObject.workflowAction.value = "close";
        value.value = "";
        var tar = document.getElementById(formObject.field.value);
        swapPosition(plainTextEditor, tar);
    });
    
    Event.on('darkmask_mask', 'dblclick', function(ev) {
    	myEditor.saveHTML();
        if (confirm("Save content?")) {
            save(editorFormName);
            editing.innerHTML = myEditor.get('element').value;
        }
    	close_editor();
    });
    
    function close_editor() {
    	Dom.setXY(myEditor.get('element_cont').get('element'), [-99999, -99999]);
        Dom.setStyle(editing, "visibility", "visible");
        
        // Fade in transition
        var darkmaskInAnim = new YAHOO.util.Anim("darkmask_mask", { opacity: {to: 0} }, 1 );
        var darkmaskHide = function() {
        	darkmask.hide();
        }
        darkmaskInAnim.onComplete.subscribe(darkmaskHide);
        darkmaskInAnim.animate();
        
        editing = null;
    }

}

// ---------- connection stuff ----------

var handleSuccess = function(o) {
    if(o.responseText !== undefined) {
    	// parse the json data in the response and display user feedback
        var result;
        var jsonString = o.responseText; // expected format {"success":false,"message":"Could not save content."}
        console.log(jsonString);
        try {
            result = YAHOO.lang.JSON.parse(jsonString);
        } catch (e) {
            result = {"success":false,"message":"Could not save content. Ajax response invalid."};
        }
        
        // FIXME: still needs to correctly parse and display the responses
        // There is also a bug in the response code, that returns a redirect to a page instead of a json response.
        if (result.success)
        	showFeedback("Content saved.");
        else
        	//showFeedback(result.message);
        	showFeedback("Error!");
    }
};

var handleFailure = function(o) {
    if(o.responseText !== undefined){
    	showFeedback("Could not save content. Ajax request failed. HTTP status: " + o.status);
    }
};

var callback = {
    success:handleSuccess,
    failure:handleFailure,
    argument:['foo','bar']
};

function save(editorFormName) {
    var formObject = document.getElementById(editorFormName); 
    YAHOO.util.Connect.setForm(formObject);     
    var sUrl = formObject.action;
    var request = YAHOO.util.Connect.asyncRequest('POST', sUrl, callback);
    
    // reset hidden form fields
    formObject.field.value="";
    formObject.workflowAction.value="";
}

function showFeedback(message) {
	var arr = message.split(/\s/);
	message = "";
	for (var i = 0; i < arr.length; i++) {
		message += encodeURIComponent(arr[i]) + " ";
	}
	
    // Put the message in three parent divs, so that the content can be vertically aligned through a css hack
	message = '<div class="vert-align1"><div class="vert-align2"><div class="vert-align3">' + message + '</div></div></div>';
	feedbackPanel.setBody(message);
	feedbackPanel.show();
    var fadeOutAnim = new YAHOO.util.Anim("feedbackPanel", { opacity: {to: 0} }, 5 );
	fadeOutAnim.animate();
}
