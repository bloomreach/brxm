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
  //{centerFrame: true, resizeFrame: false}
  drag${id}.onDragDrop = function(ev, id) {
    var callbackParameters = [{key: 'targetId', value: id}];
    ${callbackScript}
  };
}
Wicket.Event.add(window,"domready", fn_initDD${id});
