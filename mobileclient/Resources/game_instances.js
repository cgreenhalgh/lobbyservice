// List of game instances for a Game (type)
// - select item -> game_instance.js

Titanium.include('config.js');
Titanium.include('common.js');

var data = Titanium.UI.currentWindow.data;

var indexdata = [{title:'Loading...'}];

var table = Titanium.UI.createTableView({data:indexdata});

table.addEventListener('click', function(e){
	if (e.rowData.data) {
		var win = Titanium.UI.createWindow({url:'play.js',title:e.rowData.data.title});
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
	data[0] = get_index_table_row(json);
	for (var i=0; i<json.items.length; i++) {
		var row = get_details_table_row(json.items[i]);
		row.data = json.items[i];
		if (json.items[i].playUrl!=undefined)
			row.hasChild = true;
		data[i+1] = row;
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
