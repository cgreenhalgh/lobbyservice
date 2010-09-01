
var title_color = '#fff';
var desc_color = '#aaa';

// fill in client version-specifying properties in given object (for use as request to lobby)
function set_version_properties(request) {
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
}

// open browser - Android only at present
function open_browser(url) {
	// Titanium 1.5 preview (1.4.1 android_native_refactor) support for intents
	try {
		var intent = Titanium.Android.createIntent({action:Titanium.Android.ACTION_VIEW,data:url});
		var activity = Titanium.Android.createActivity(intent);
		activity.start(intent);
	}
	catch (err) {
		alert('Sorry - Unable to open browser');
	}
}

function get_details_description(index) {
	var description = '';
	if (index.subtitle!=undefined) {
		description = description+index.subtitle+'\n';
	}
	if (index.locationName!=undefined) {
		description = description+'At: '+index.locationName+'\n';
	}
	if (index.startTime!=undefined) {
		description = description+'From: '+new Date(index.startTime)+'\n';
	}
	if (index.endTime!=undefined) {
		description = description+'From: '+new Date(index.endTime)+'\n';
	}
	return description;
}

var rowi = 0;
function get_table_class() {
	rowi = rowi+1;
	return 'row'+rowi;
}
//create a table view for an index top-level
function get_header_view(title, imageUrl) {
	var tableRow = Titanium.UI.createView({
			height:'auto',
			width:Titanium.UI.currentWindow.size.width-70,
			top:0,
			left:0
		});
	if (imageUrl!=null && imageUrl!=undefined) {
		var imageView = Titanium.UI.createImageView({
			image:imageUrl,
			width:32,
			height:32,
			canScale:true,
			enableZoomControls:false,
			left:0,
			top:0,
			borderWidth:4
		});
		tableRow.add(imageView);
	}

	var title = Titanium.UI.createLabel({
		text:title,
		textAlign:'left',
		font:{fontSize:16},
		color:title_color,
		left:37,
		top:0,
//		width:'auto',
		height:'auto'
	});
	tableRow.add(title);
	return tableRow;
}
function get_index_header_view(index) {
	return get_header_view(index.title, index.imageUrl);
}
function get_details_header_view(index) {
	var description = Titanium.UI.createLabel({
		text:get_details_description(index),
		textAlign:'left',
		font:{fontSize:12},
		color:title_color,
		left:0,
		top:0,
		//height:'auto',
		width:Titanium.UI.currentWindow.size.width-70,
		//width:'auto'
			//borderWidth:10
	});
	return description;
}
function get_detail_view(description, link) {
	var tableRow = Titanium.UI.createView({
		height:'auto',
		width:Titanium.UI.currentWindow.size.width-70,
		top:0,
		left:0
	});

	if (link!=null && link!=undefined) {
		var linkView = Titanium.UI.createImageView({
			url:'www_50.png',
			canScale:true,
			enableZoomControls:false,
			width:32,
			height:32,
			borderWidth:4,
			left:0,
			top:0
		});
		tableRow.add(linkView);
		linkView.addEventListener('click',function(e) {
			open_browser(link);
		});
	}
	if (description!=undefined) {
		var description = Titanium.UI.createLabel({
			text:description,
			textAlign:'left',
			font:{fontSize:12},
			color:desc_color,
			left:37,
			top:0,
			//height:'auto',
			width:Titanium.UI.currentWindow.size.width-70-21,
			width:'auto'
				//borderWidth:10
		});
		tableRow.add(description);		
	}
	//tableRow.hasChild = false;
	return tableRow;
}
function get_index_detail_view(index) {
	return get_detail_view(index.description, index.link);
}

//==================================================================
//Time utilities

//number leading 0 format helper
function format(text,len) {
	var str = String(text);
	while (str.length<2) 
 	str = '0'+str;
	return str;
}
//date to (relatively) pretty text format
function prettyTimeToString(time) {
	if (time==0)
		return "Unspecified";
	var date = new Date(time);
	// TZ?
	var str = format(date.getUTCFullYear(),4)+'-'+format(date.getUTCMonth()+1,2)+'-'+format(date.getUTCDate(),2)+' '+
	format(date.getUTCHours(),2)+':'+format(date.getUTCMinutes(),2)+':'+format(date.getUTCSeconds(),2);
	if (date.getTimezoneOffset()!=0)
		str = str+' Z ('+date.getTimezoneOffset()+')';
		//date.toUTCString();
	return str;
}
//date to short text format
function timeToString(time) {
	var date = new Date(time);
	var str = format(date.getUTCFullYear(),4)+format(date.getUTCMonth()+1,2)+format(date.getUTCDate(),2)+'T'+
	format(date.getUTCHours(),2)+format(date.getUTCMinutes(),2)+format(date.getUTCSeconds(),2)+'Z';
	return str;
}
//short text format to date
function stringToTime(str) {
	var date = new Date(0);
	var match = /\d\d\d\d\d\d\d\d[T]\d\d\d\d\d\d[Z]/ .exec(str);
	if (match==null) 
		alert('Unable to parse time '+str);
	else {
		date.setUTCFullYear(str.substr(0,4));
		date.setUTCMonth(Number(str.substr(4,2))-1);
		date.setUTCDate(str.substr(6,2));
		date.setUTCHours(str.substr(9,2));
		date.setUTCMinutes(str.substr(11,2));
		date.setUTCSeconds(str.substr(13,2));
	}
	return date.getTime();				
}
