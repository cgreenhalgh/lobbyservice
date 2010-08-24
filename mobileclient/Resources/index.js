// game index window

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

Titanium.UI.currentWindow.add(table);

var client = Titanium.Network.createHTTPClient();
client.setTimeout(30000);
client.open('GET','http://128.243.22.74:8888/browser/GetGameIndex',true);
client.onload = function() {
	var json = JSON.parse(client.responseText);
	var data = [];
	data[0] = {title:json.title+' (server)'};
	if (json.imageUrl!=undefined)
		data[0].leftImage = json.imageUrl;
	for (var i=0; i<json.items.length; i++) {
		var row = {title:json.items[i].title+' (game)',hasChild:true};
		if (json.items[i].imageUrl!=undefined)
			row.leftImage = json.items[i].imageUrl;
		data[i+1] = row;
	}
	table.setData(data);
};
client.onerror = function() {
	table.setData([{title:'Error - '+client.status}]);
};
client.send();