// details of a single game instance
// - button (check client) -> market place
// - button (join) -> start app


Titanium.include('config.js');
Titanium.include('common.js');

Titanium.UI.currentWindow.layout='vertical';

var data = Titanium.UI.currentWindow.data;
var clientinfo = data.clientTemplates[0];
/*
var label = Titanium.UI.createLabel({
	text:'Title: '+data.title+'\n'+
	'Description: '+data.description+'\n'+
	'Data: '+JSON.stringify(data)+'\n'+
	'Join URL: '+data.queryUrl+'\n'+
	'Client: '+clientinfo+'\n',
	font:{fontSize:14},
	left:10,
//	top:10,
	width:'auto',
	height:'auto'
});

Titanium.UI.currentWindow.add(label);
*/
// not a table view
var template_view = get_index_table_row(data, Titanium.UI.createView({height:'auto'}));

Titanium.UI.currentWindow.add(template_view);

//not a table view
var detail_view = get_detail_table_row(data, Titanium.UI.createView({height:'auto'}));

Titanium.UI.currentWindow.add(detail_view);

var clientButton = Titanium.UI.createButton({
//	left:100,
//	top:10,
	title:'Check Client',
	width:'auto',
	height:'auto'
});

if (clientinfo.applicationMarketId!=undefined)
	Titanium.UI.currentWindow.add(clientButton);

clientButton.addEventListener('click', function(e) {
	// Titanium 1.5 preview (1.4.1 android_native_refactor) support for intents
	// Note: this doesn't work on the emulator!
	try {
		var intent = Titanium.Android.createIntent({action:Titanium.Android.ACTION_VIEW,data:'market://details?id='+clientinfo.applicationMarketId});
		var activity = Titanium.Android.createActivity(intent);
		activity.start(intent);
	}
	catch (err) {
		Titanium.UI.createAlertDialog({title:'Sorry',message:'Could not open the marketplace'}).show();
	}
});


var button = Titanium.UI.createButton({
	left:10,
//	top:10,
	title:'Join game...',
	width:'auto',
	height:'auto'
});


function join(e) {
	var client = Titanium.Network.createHTTPClient();
	client.setTimeout(30000);
	client.open('POST',data.joinUrl,true);
	client.onload = function() {
		var json = JSON.parse(client.responseText);
		Titanium.API.log('INFO','Join: '+client.responseText);
		// Titanium 1.5 preview (1.4.1 android_native_refactor) support for intents
		// this starts an activity (only intent action & data are supported, plus putExtra)
//		var intent = Titanium.Android.createIntent({action:Titanium.Android.ACTION_VIEW,data:'http://www.mrl.nott.ac.uk/'});
//		var activity = Titanium.Android.createActivity(intent);
//		activity.start(intent);
		if (json.status=='OK') {
			try {
				var intent = Titanium.Android.createIntent({action:clientinfo.applicationLaunchId});
				intent.putExtra('playUrl',json.playUrl);
				intent.putExtra('clientId',json.clientId);
				intent.putExtra('nickname',json.nickname);
				for (var p in json.playData) {
					intent.putExtra(p,json.playData[p]);
				}
				var activity = Titanium.Android.createActivity(intent);
				Titanium.API.log('INFO','start '+intent);
				activity.start(intent);
			}
			catch (err) {
				Titanium.API.log('WARNING','Could not start client '+clientInfo.applicationLaunchId+': '+err);
				Titanium.UI.createAlertDialog({title:'Sorry',message:'Could not start the game client'}).show();
			}
		}		
		else {
			Titanium.UI.createAlertDialog({title:'Sorry',message:'Could not join the game ('+json.status+')'}).show();
		}	
	};
	client.onerror = function() {
		Titanium.API.log('ERROR','Join error');
		Titanium.UI.createAlertDialog({title:'Sorry',message:'Could not join the game ('+client.status+')'}).show();
	};
	// include deviceId as default anon client Id
	var request = {version:1,type:'PLAY',deviceId:Titanium.Platform.id};
	set_version_properties(request);
	Titanium.API.log('INFO',"Send join "+JSON.stringify(request)+" to "+data.joinUrl);
	client.send(JSON.stringify(request));
}

button.addEventListener('click', join);

if (data.joinUrl!=undefined)
	Titanium.UI.currentWindow.add(button);
