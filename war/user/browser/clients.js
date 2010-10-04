// JS for clients.html

var clientId = null;
var time = null;
var hmac = null;

// on load
$(document).ready(function() {
	
	clientId = gup('clientId');
	time = gup('time');
	hmac = gup('hmac');
	
	alert('clientId = '+clientId);
	
});
