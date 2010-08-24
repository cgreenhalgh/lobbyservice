// game instance list window
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
	data[0] = {title:json.title+' (server)'};
	if (json.imageUrl!=undefined)
		data[0].leftImage = json.imageUrl;
	for (var i=0; i<json.items.length; i++) {
		var row = {title:json.items[i].title+' (instance)',hasChild:true,data:json.items[i]};
		if (json.items[i].imageUrl!=undefined)
			row.leftImage = json.items[i].imageUrl;
		data[i+1] = row;
	}
	table.setData(data);
};
client.onerror = function() {
	table.setData([{title:'Error - '+client.status}]);
};
var request = {version:1};
if (Titanium.Platform.name=='android')
	request.clientType = 'ANDROID';
else
	request.clientType = Titanium.Platform.name;
var version = Titanium.Platform.version;
var dotix = version.indexOf('.', 0);
if (dotix<0)
	request.majorVersion = Number(version);
else {
	request.majorVersion = Number(version.substr(0,dotix));
	var dotix2 = version.indexOf('.',dotix+1);
	// e.g. 2.1-update1
	var hypix2 = version.indexOf('-', dotix+1)
	if (hypix2>=0 && (dotix2<0 || dotix2>hypix2))
		dotix2 = hypix2;
	if (dotix2<0) {
		Titanium.API.log('INFO','minorVersion='+version.substr(dotix+1)+', dotix='+dotix+', dotix2='+dotix2);
		request.minorVersion = Number(version.substr(dotix+1));
	}
	else {
		Titanium.API.log('INFO','minorVersion='+version.substr(dotix+1,dotix2-dotix-1)+', dotix='+dotix+', dotix2='+dotix2);
		request.minorVersion = Number(version.substr(dotix+1,dotix2-dotix-1));
	}
}
Titanium.API.log("Send query "+JSON.stringify(request)+" to "+data.queryUrl);
client.send(JSON.stringify(request));
