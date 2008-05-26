function fn_initTarget${id}(){
  var drop${id} = null;
  if('${group}' == '')
    drop${id} = new YAHOO.util.DDTarget('${id}');
  else 
    drop${id} = new YAHOO.util.DDTarget('${id}', '${group}');
    
  var moreGroups = ${moreGroups};
  if(moreGroups != null) {
    for(var i=0; i<moreGroups.length; i++) {
      drop${id}.addToGroup(moreGroups[i]);
    }
  }
    
}
Wicket.Event.add(window,"domready", fn_initTarget${id});
