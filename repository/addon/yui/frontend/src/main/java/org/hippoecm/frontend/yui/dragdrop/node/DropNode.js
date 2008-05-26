function fn_initDropNode${id}(){
    if('${group}' == '')
      drop${id} = new YAHOO.util.DDTarget('${id}');
    else 
      drop${id} = new YAHOO.util.DDTarget('${id}', '${group}');
}
Wicket.Event.add(window,"domready", fn_initDropNode${id});
