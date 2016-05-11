var tz = jstz.determine();
var timezoneSelect = $("#timezone");

if (timezoneSelect.prop("selectedIndex") == 0) {
  timezoneSelect.val(tz.name());
}