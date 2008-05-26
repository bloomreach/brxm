function fn_initDrag${id}(){
  drag${id} = new YAHOO.hippo.DDNode('${id}', '${group}', {label: '${label}', centerFrame: true, resizeFrame: false});
  drag${id}.onDragDrop = function(ev, id) {
    var callbackParameters = [{key: 'targetId', value: id}];
    ${callbackScript}
  };
}
Wicket.Event.add(window,"domready", fn_initDrag${id});
