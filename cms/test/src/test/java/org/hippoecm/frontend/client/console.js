(function() {
	window.console = {
		log: function(out) {
			window.opener.log(out);
		}
	};
})();
