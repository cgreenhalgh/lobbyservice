// Global Index of Game (types)
// - select a Game -> game_template.js

Titanium.include('config.js');
Titanium.include('common.js');
/*
var label = Titanium.UI.createLabel({
	text:'Game Index...',
	textAlign:'left',
	width:'auto',
	height:'auto'
});
Titanium.UI.currentWindow.add(label);
*/
var indexdata = [{title:'Loading...'}];

var table = Titanium.UI.createTableView({data:indexdata});

table.addEventListener('click', function(e){
	if (e.rowData.data) {
		var win = Titanium.UI.createWindow({url:'game_template.js',title:e.rowData.data.title});
		win.data = e.rowData.data;
		win.open({modal:true});
	}
});

Titanium.UI.currentWindow.add(table);

var client = Titanium.Network.createHTTPClient();
client.setTimeout(30000);
client.open('GET',lobbyUrl+'/browser/GetGameIndex',true);
client.onload = function() {
	var json = JSON.parse(client.responseText);
	var data = [];
	data[0] = get_index_table_row(json);
	for (var i=0; i<json.items.length; i++) {
		var row = get_index_table_row(json.items[i]);
		row.data = json.items[i];
		if (json.items[i].queryUrl!=undefined)
			row.hasChild = true;
		data[i+1] = row;
	}
	table.setData(data);
};
client.onerror = function() {
	table.setData([{title:'Error - '+client.status}]);
	Titanium.UI.createAlertDialog({title:'Sorry',message:'Could not get the game index ('+client.status+')'}).show();
};
client.send();