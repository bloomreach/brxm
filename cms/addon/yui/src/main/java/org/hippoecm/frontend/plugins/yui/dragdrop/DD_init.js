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
 
function fn_initDD${id}(){
  var drag${id} = null;
  if('${group}' == '')
    drag${id} = new YAHOO.util.DD('${id}');
  else
    drag${id} = new YAHOO.util.DD('${id}', '${group}');
  
  var moreGroups = ${moreGroups};
  if(moreGroups != null) {
    for(var i=0; i<moreGroups.length; i++) {
      drag${id}.addToGroup(moreGroups[i]);
    }
  }
  drag${id}.onDragDrop = function(ev, id) {
    var callbackParameters = [{key: 'targetId', value: id}];
    ${callbackScript}
  };
}
Wicket.Event.add(window,"domready", fn_initDD${id});
