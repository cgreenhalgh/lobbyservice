var data = Titanium.UI.currentWindow.data;
var label = Titanium.UI.createLabel({
	text:'Title: '+data.title+'\n'+
	'Description: '+data.description+'\n'+
	'Query URL: '+data.queryUrl+'\n'+
	'Data: '+JSON.stringify(data)+'\n'+
	'Platform device id: '+Titanium.Platform.id+'\n'+
	'Platform name: '+Titanium.Platform.name+'\n'+ // e.g. 'android'
	'Platform model: '+Titanium.Platform.model+'\n'+ // e.g. 'Nexus One'
	'Platform locale: '+Titanium.Platform.locale+'\n'+ // e.g. 'en'
	'Platform osname: '+Titanium.Platform.osname+'\n'+ // e.g. 'android'
	'Platform version: '+Titanium.Platform.version+'\n', // e.g. '2.2'
	font:{fontSize:14},
	left:10,
	top:10,
	width:'auto',
	height:'auto'
});

Titanium.UI.currentWindow.add(label);

var button = Titanium.UI.createButton({
	left:10,
	top:10,
	title:'Find games...'
});

button.addEventListener('click', function(e) {
	var win = Titanium.UI.createWindow({url:'game_list.js',title:data.title});
	win.data = data;
	win.open({modal:true});
});

if (data.queryUrl!=undefined)
	Titanium.UI.currentWindow.add(button);

