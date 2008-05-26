var myLogReader = null;
function initLogger() {
  if(myLogReader == null)
    myLogReader = new YAHOO.widget.LogReader();
}
//YAHOO.util.Event.addListener(window, "load", initLogger);
Wicket.Event.add(window,"domready", initLogger);