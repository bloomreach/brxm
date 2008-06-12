/*
 * Copyright 2007 Hippo
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
 
 function AjaxLoadIndicator(elId) {
    this.elId = elId;
    this.calls = 0;
}

AjaxLoadIndicator.prototype.getElement = function() {
    return YAHOO.util.Dom.get(this.elId);
};

AjaxLoadIndicator.prototype.show = function() {
    this.calls++;
    YAHOO.util.Dom.setStyle(this.getElement(), 'display', 'block');
}

AjaxLoadIndicator.prototype.hide = function() {
    if(this.calls > 0) {
        this.calls--;
    } 
    if (this.calls == 0) {
        YAHOO.util.Dom.setStyle(this.getElement(),'display', 'none');
    }
};