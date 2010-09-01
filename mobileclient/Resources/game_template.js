// Details of a Game Type
// - button (find games) -> game_instances.js

Titanium.include('config.js');
Titanium.include('common.js');

Titanium.UI.currentWindow.layout='vertical';

var latitudeE6 = undefined;
var longitudeE6 = undefined;
var location = null;
var locationDialog = null;

if (Titanium.Geolocation.locationServicesEnabled!=false) {
	
	function onLocation(e) {
		
		Titanium.API.info('Got location: '+JSON.stringify(e.coords));

		if (e.error)
		{
			alert('Problem getting location: '+JSON.stringify(e.error));
			return;
		}
		
		location = e.coords;
		latitudeE6 = Math.round(location.latitude*1000000);
		longitudeE6 = Math.round(location.longitude*1000000);
		if (locationDialog!=null)
			locationDialog.close();
	
		/*try {
			Titanium.Geolocation.removeEventListener('location', this);
		}
		catch (err) {
			Titanium.API.error('unregistering for geolocation (in callback): '+err);
		}*/
	};
	/*Titanium.UI.currentWindow.addEventListener('close', function () {
		try {
			Titanium.Geolocation.removeEventListener('location', onLocation);
		}
		catch (err) {
			Titanium.API.error('unregistering for geolocation: '+err);
		}
		*/
	Titanium.Geolocation.accuracy = Titanium.Geolocation.ACCURACY_BEST;
	Titanium.Geolocation.getCurrentPosition(onLocation);
};
/*
try {
	Titanium.Geolocation.addEventListener('location', onLocation);
}
catch (err) {
	Titanium.API.error('registering for geolocation: '+err);
}
*/
var data = Titanium.UI.currentWindow.data;
/*var label = Titanium.UI.createLabel({
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
//	top:10,
	width:'auto',
	height:'auto'
});

Titanium.UI.currentWindow.add(label);
*/
//not a table view
var win = Titanium.UI.currentWindow;

win.add(get_index_header_view(data));
win.add(Titanium.UI.createView({height:10}));
win.add(get_index_detail_view(data));
win.add(Titanium.UI.createView({height:10}));

// TODO Clients...
var options = [];
var includeCurrentRow = Titanium.UI.createTableViewRow({
	title:'Include Current Games',
	hasCheck:true,
	font:{fontSize:14},
	color:'#fff'
});
includeCurrentRow.data = 'includeCurrent';
options.push(includeCurrentRow);

/*var minTime = new Date().getTime();

var minTimeRow = Titanium.UI.createTableViewRow({
	title:'From: '+prettyTimeToString(minTime),
	font:{fontSize:14},
	color:'#666', // 'disabled'
	hasCheck:false
});
minTimeRow.data = 'minTime';
options.push(minTimeRow);
*/
//                           Titanium.Geolocation.locationServicesEnabled
var includeLocationRow;
if (Titanium.Geolocation.locationServicesEnabled!=false) {
	includeLocationRow = Titanium.UI.createTableViewRow({
		title:'Send My Location',
		hasCheck:true,
		font:{fontSize:14},
		color:'#fff'
	});
	includeLocationRow.data = 'includeLocation';
	options.push(includeLocationRow);
}
else {
	includeLocationRow = Titanium.UI.createTableViewRow({
		title:'Location services are off',
		hasCheck:false,
		font:{fontSize:14},
		color:'#888'
	});
	//includeLocationRow.data = 'includeLocation';
	options.push(includeLocationRow);	
}

var now = new Date().getTime();
var timeOptions = ['Any Time','Now','Today','Tomorrow','Within a week'];
// stop at 2am? Local time
var midnight = new Date(now);
midnight.setHours(0); midnight.setMinutes(0); midnight.setSeconds(0); midnight.setMilliseconds(0);
var timeOfDay = now-midnight;
var timeOptionMin = [now, now, now, now-timeOfDay+24*60*60*1000, now];
var timeOptionMax = [0, now+5*60*1000, now-timeOfDay+24*60*60*1000, now-timeOfDay+2*24*60*60*1000, 
                     now-timeOfDay+7*24*60*60*1000];
var timeOptionIx = 0;

var timeOptionRow = Titanium.UI.createTableViewRow({
	title:timeOptions[timeOptionIx],
	//hasCheck:true,
	font:{fontSize:14},
	color:'#fff'
});
timeOptionRow.data = 'timeOption';
options.push(timeOptionRow);

var rangeOptions = ['Anywhere', 'Here!', 'Up to 1.5 km', 'Up to 5 km', 'Up to 15 km', 'Up to 50 km', 'Up to 150 km', 'Up to 500 km'];
var rangeOptionIx = 0;
var rangeOptionMetres = [-1, 0, 1500, 5000, 15000, 50000, 150000, 500000];

var rangeOptionRow;
if (Titanium.Geolocation.locationServicesEnabled!=false) {
	rangeOptionRow = Titanium.UI.createTableViewRow({
		title:rangeOptions[rangeOptionIx],
		//hasCheck:true,
		font:{fontSize:14},
		color:'#fff'
	});
	rangeOptionRow.data = 'rangeOption';
	options.push(rangeOptionRow);
}                       
else 
{
	rangeOptionRow = Titanium.UI.createTableViewRow({
		title:rangeOptions[rangeOptionIx],
		//hasCheck:true,
		font:{fontSize:14},
		color:'#888'
	});
//	rangeOptionRow.data = 'rangeOption';
	options.push(rangeOptionRow);	
}

var optionTable = Titanium.UI.createTableView();
optionTable.setData(options);
win.add(optionTable);

// this doesn't work at the moment!
/*
function pickDate(time, callback) {
	// createPicker does not exist on Android...?! at least on 1.4.0
	// apparently Date and Time (but not both together) are due in 1.5.0
	//var picker = Titanium.UI.createPicker({type:Titanium.UI.PICKER_TYPE_DATE_AND_TIME,value:time});
	var dialog = Titanium.UI.createWindow({
		layout:'vertical'//,borderRadius:10,borderColor:'#aaa',borderWidth:3
	});
	//dialog.add(picker);
	var years = [{title:'2010',font:{fontSize:16},color:'#fff',data:2010},
	             {title:'2011',font:{fontSize:16},color:'#fff',data:2011}];
	var yearTable = Titanium.UI.createTableView({
		data:years,
		height:50
	});
	var date = new Date(time);
	var yeari = 0;
	years[yeari].hasCheck = true;//Not implemented: yearTable.selectRow(yeari);
	yearTable.updateRow(yeari,years[yeari]);
	yearTable.scrollToIndex(yeari);
	yearTable.addEventListener('click', function(e) {
		years[yeari].hasCheck = false;//Not implemented: yearTable.deselectRow(yeari);
		yearTable.updateRow(yeari,years[yeari]);
		yeari = e.index;
		//yearTable.selectRow(yeari);
		years[yeari].hasCheck = true;//Not implemented: yearTable.deselectRow(yeari);
		yearTable.updateRow(yeari,years[yeari]);
		yearTable.scrollToIndex(yeari);
	});
	dialog.add(yearTable);
	var buttons = Titanium.UI.createView();
	var cancel = Titanium.UI.createButton({
		title:'OK',left:0,top:0,height:'auto',width:'auto'
	});
	cancel.addEventListener('click',function(e) {
		dialog.close();
	});
	buttons.add(cancel);
	var rval = undefined;
	var ok = Titanium.UI.createButton({
		title:'Cancel',right:0,top:0,height:'auto',width:'auto'
	});
	ok.addEventListener('click',function(e) {
		//rval = picker.value;
		dialog.close();
	});
	buttons.add(ok);
	dialog.add(buttons);
	dialog.addEventListener('close',function() {
		if (callback!=null && callback!=undefined)
			callback(rval);
	});
	dialog.open({modal:true});
}
*/
//table option events
optionTable.addEventListener('click', function(e) {
	//alert('click '+e.rowData.data);
	if (e.rowData.data=='includeCurrent') {
		//alert('click includeCurrent');
		
		includeCurrentRow.hasCheck = !includeCurrentRow.hasCheck;
		//minTimeRow.hasCheck = !includeCurrentRow.hasCheck;
		//if (minTimeRow.hasCheck)
		//	minTimeRow.color = '#fff';
		//else
		//	minTimeRow.color = '#666';
		//optionTable.updateRow(includeCurrentRow);
		//optionTable.updateRow(minTimeRow);
	} 
	else if (e.rowData.data=='includeLocation') {
		//alert('click includeCurrent');
		
		includeLocationRow.hasCheck = !includeLocationRow.hasCheck;
		//minTimeRow.hasCheck = !includeCurrentRow.hasCheck;
		//if (minTimeRow.hasCheck)
		//	minTimeRow.color = '#fff';
		//else
		//	minTimeRow.color = '#666';
		//optionTable.updateRow(includeCurrentRow);
		//optionTable.updateRow(minTimeRow);
	} 
	else if (e.rowData.data=='timeOption') {
		var optionDialog = Titanium.UI.createOptionDialog({
			title:'Game time',
			options:timeOptions
		});
		optionDialog.addEventListener('click',function(e) {
			if (e.cancel) {
				// debug
				alert('cancel');
			}
			if (e.index>=0 && e.index<timeOptions.length) {
				timeOptionIx = e.index;
				timeOptionRow.title = timeOptions[timeOptionIx];
				// warning: fixed index!
				optionTable.updateRow(2, timeOptionRow);
			}
			else
				alert('index out of range: '+e.index);
		});
		optionDialog.show();
	}
	else if (e.rowData.data=='rangeOption') {
		var optionDialog = Titanium.UI.createOptionDialog({
			title:'Game Location',
			options:rangeOptions
		});
		optionDialog.addEventListener('click',function(e) {
			if (e.cancel) {
				// debug
				alert('cancel');
			}
			if (e.index>=0 && e.index<rangeOptions.length) {
				rangeOptionIx = e.index;
				rangeOptionRow.title = rangeOptions[rangeOptionIx];
				// warning: fixed index!
				optionTable.updateRow(3, rangeOptionRow);
			}
			else
				alert('index out of range: '+e.index);
		});
		optionDialog.show();
	}
/*	else if (e.rowData.data=='minTime') {
		if (minTimeRow.hasCheck) {
			var t = pickDate(minTime);
			if (t!=null && t!=undefined) {
				minTime = t;
				minTimeRow.title = 'From: '+prettyTimeToString(minTime);
				//?
				optionTable.updateRow(minTimeRow);
			}
		}
	}
*/
});

var button = Titanium.UI.createButton({
	left:10,
	//top:10,
	title:'Find games...',
	width:'auto',
	height:'auto'
});

function doQuery(e) {
	
	if ((includeLocationRow.hasCheck || rangeOptionMetres[rangeOptionIx]) && location==null) {
		if (e!=null && e!=undefined) {
			// call from button
			// wait for location
			Titanium.API.info('Waiting for location');
			var dialog = Titanium.UI.createActivityIndicator({message:'Getting location...'});
			dialog.addEventListener('close', doQuery);
			setTimeout(function() {
				try {
					dialog.close();
				}
				catch (err) {}
			}, 5000);
			dialog.show();
			return;
		}
		else {
			alert('Sorry - couldn\'t get your location');
			return;
		}
	}
	
	var win = Titanium.UI.createWindow({url:'game_instances.js',title:data.title});
	win.data = data;
	var timeConstraint = {
			includeStarted:includeCurrentRow.hasCheck
	};
	if (timeOptionMax[timeOptionIx]!=0) {
		timeConstraint.minTime = timeOptionMin[timeOptionIx];
		timeConstraint.maxTime = timeOptionMax[timeOptionIx];
	}
	win.timeConstraint = timeConstraint;
	if (rangeOptionMetres[rangeOptionIx]>=0) {
		
		var locationConstraint = {
				type:'CIRCLE',
				radiusMetres:rangeOptionMetres[rangeOptionIx]
		};
		locationConstraint.latitudeE6 = latitudeE6;
		locationConstraint.longitudeE6 = longitudeE6;
		win.locationConstraint = locationConstraint;
	}
	if (includeLocationRow.hasCheck) {
		win.latitudeE6 = latitudeE6;
		win.longitudeE6 = longitudeE6;
	}
	win.open({modal:true});
};

button.addEventListener('click', doQuery);

if (data.queryUrl!=undefined)
	win.add(button);

