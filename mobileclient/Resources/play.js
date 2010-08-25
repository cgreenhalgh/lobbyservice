var data = Titanium.UI.currentWindow.data;
var clientinfo = data.clientTemplates[0];

var label = Titanium.UI.createLabel({
	text:'Title: '+data.title+'\n'+
	'Description: '+data.description+'\n'+
	'Data: '+JSON.stringify(data)+'\n'+
	'Join URL: '+data.queryUrl+'\n'+
	'Client: '+clientinfo+'\n',
	font:{fontSize:14},
	left:10,
	top:10,
	width:'auto',
	height:'auto'
});

Titanium.UI.currentWindow.add(label);

var clientButton = Titanium.UI.createButton({
	left:100,
	top:10,
	title:'Check Client'
});

if (clientinfo.applicationMarketId!=undefined)
	Titanium.UI.currentWindow.add(clientButton);

clientButton.addEventListener('click', function(e) {
	// Titanium 1.5 preview (1.4.1 android_native_refactor) support for intents
	// Note: this doesn't work on the emulator!
	var intent = Titanium.Android.createIntent({action:Titanium.Android.ACTION_VIEW,data:'market://details?id='+clientinfo.applicationMarketId});
	var activity = Titanium.Android.createActivity(intent);
	activity.start(intent);
});


var button = Titanium.UI.createButton({
	left:10,
	top:10,
	title:'Join game...'
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
	};
	client.onerror = function() {
		Titanium.API.log('ERROR','Join error');
	};
	// include deviceId as default anon client Id
	var request = {version:1,type:'PLAY',deviceId:Titanium.Platform.id};
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
		if (dotix2<0)
			request.minorVersion = Number(version.substr(dotix+1));
		else {
			request.minorVersion = Number(version.substr(dotix+1,dotix2-dotix-1));
		}
	}
	Titanium.API.log('INFO',"Send join "+JSON.stringify(request)+" to "+data.joinUrl);
	client.send(JSON.stringify(request));
}

button.addEventListener('click', join);

if (data.joinUrl!=undefined)
	Titanium.UI.currentWindow.add(button);
