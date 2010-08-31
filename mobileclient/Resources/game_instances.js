// List of game instances for a Game (type)
// - select item -> game_instance.js

Titanium.include('config.js');
Titanium.include('common.js');

var data = Titanium.UI.currentWindow.data;

var indexdata = [{title:'Loading...'}];

var table = Titanium.UI.createTableView({data:indexdata});

table.addEventListener('click', function(e){
	if (e.rowData.data) {
		var win = Titanium.UI.createWindow({url:'game_instance.js',title:e.rowData.data.title});
		win.data = e.rowData.data;
		win.open({modal:true});
	}
});

Titanium.UI.currentWindow.add(table);

var client = Titanium.Network.createHTTPClient();
client.setTimeout(30000);
client.open('POST',data.queryUrl,true);
client.onload = function() {
	var json = JSON.parse(client.responseText);
	var data = [];
	if (json.items.length>0) {
		var row = Titanium.UI.createTableViewRow();
		row.add(get_index_header_view(json.items[0]));
		data.push(row);
		row = Titanium.UI.createTableViewRow();
		row.add(get_index_detail_view(json.items[0]));
		data.push(row);
	}
	for (var i=0; i<json.items.length; i++) {
		var row = Titanium.UI.createTableViewRow();
		row.add(get_details_header_view(json.items[i]));
		row.data = json.items[i];
		if (json.items[i].playUrl!=undefined)
			row.hasChild = true;
		data.push(row);
	}
	table.setData(data);
};
client.onerror = function() {
	table.setData([{title:'Error - '+client.status}]);
	Titanium.UI.createAlertDialog({title:'Sorry',message:'Could not get the game index ('+client.status+')'}).show();
};
var request = {version:1};
set_version_properties(request);

Titanium.API.log("Send query "+JSON.stringify(request)+" to "+data.queryUrl);
client.send(JSON.stringify(request));
