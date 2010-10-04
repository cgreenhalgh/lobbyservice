// JS for clients.html
$.ajaxSetup({cache:false,async:true,timeout:30000});

// passed in arguments, e.g. referral from client
var clientId = null;
var time = null;
var hmac = null;

//on load
$(document).ready(function() {
	
	clientId = gup('clientId');
	time = gup('time');
	hmac = gup('hmac');
	
	//alert('clientId = '+clientId);
	if (clientId!=undefined && clientId!=null) {
		// ...
		$('#currentClient').html('Client: '+clientId);
	}
	else 
		$('#client_div').hide('fast');
		
	// get clients
	get_clients();
	
});

function get_clients() {
	var table = $('#clients');
	$('tr',table).remove();
	table.append('<tr><td>Loading...</td></tr>');
	try {
		$.ajax({url: '../GetUserGameClients', 
			type: 'GET',
			contentType: 'application/json',
			processData: false,
			dataType: 'json',
			success: function success(data, status) {
				log('get clients resp. '+$.toJSON(data));
				update_clients(data);
			},
			error: function error(req, status) {
				error_clients(status);
			}
		});
	} catch (err) {
		error_clients(err.message);//$.toJSON(err));
//		alert('Error attempting query on '+item.queryUrl+': '+err);
	}
}

function error_clients(msg) {
	var table = $('#clients');
	$('tr',table).remove();
	table.append('<tr><td>Sorry - '+msg+'</td></tr>');
}

function update_clients(clients) {
	var table = $('#clients');
	$('tr',table).remove();
	for (var i=0; i<clients.length; i++) {
		var client = clients[i];
		if (client.id==clientId) {
			$('#currentClient').html(client.status+ ' client: '+clientId);
			$('input[name=block]').attr('disabled', client.status=='BLOCKED');
			$('input[name=trust]').attr('disabled', client.status!='ANONYMOUS');
		}
		table.append('<tr><td>'+$.toJSON(client)+'</td></tr>');
	}
}

function do_trust() {
	if (confirm('Trust this client?')) {
		do_client_management('TRUSTED');
	}
}
function do_block() {
	if (confirm('Block this client? (no undo)')) {
		do_client_management('BLOCKED');
	}
}

function do_client_management(newStatus) {
	var request = {version:1,newStatus:newStatus,clientId:clientId};
	if (time!=null)
		request.clientTime = time;
	if (hmac!=null)
		request.clientHmac = hmac;
	try {
		var url = '../ClientManagement';
		var data = $.toJSON(request);
		log('Send '+data+' to '+url);
		$.ajax({url: url,
			type: 'POST',
			contentType: 'application/json',
			processData: false,
			data: data,
			dataType: 'json',
			success: function (data, status) {
				log('client mgmt resp. '+$.toJSON(data));
				if (data.status=='OK') {
					alert('Done');
					get_clients();
				}
				else
					alert('Sorry: '+data.message);
			},
			error: function (req, status) {
				log('client mgmt error '+status+' ('+req.status+', '+req.statusText+')');
				alert('Sorry - '+status+' ('+req.status+', '+req.statusText+')');
			}
		});
	} catch (e) {
		log('client mgmt exception '+e.type+': '+e.message);
		alert('Sorry - '+e.type+': '+e.message);
	}
}

