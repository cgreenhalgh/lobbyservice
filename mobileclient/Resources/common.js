
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

// create a table view for an index top-level
// tableRow is optional view arg
function get_details_table_row(index, tableRow) {
	if (tableRow==undefined || tableRow==null)
		tableRow = Titanium.UI.createTableViewRow({height:'auto'});
	if (index.imageUrl!=undefined) {
		var imageView = Titanium.UI.createImageView({
			url:index.imageUrl,
			width:50,
			height:50,
			canScale:true,
			enableZoomControls:false,
			left:10,
			top:10,
			borderWidth:10
		});
		tableRow.add(imageView);
	}
	if (index.link!=undefined) {
		var link = Titanium.UI.createButton({
			title:'Web',
			width:'auto',
			height:'auto',
			borderWidth:10,
			left:70,
			top:10
		});
		tableRow.add(link);
		link.addEventListener('click',function(e) {
			open_browser(index.link);
		});
	}
	var centreView = Titanium.UI.createView({
		left:120,
		top:10,
		height:'auto',
		layout:'vertical'
	});
	if (index.subtitle!=undefined) {
		var title = Titanium.UI.createLabel({
			text:index.title,
			textAlign:'left',
			font:{fontSize:16},
			left:0,
			height:'auto'
		});
		centreView.add(title);
	}
	if (index.startTime!=undefined) {
		var l = Titanium.UI.createLabel({
			text:'Start: '+index.startTime,
			textAlign:'left',
			font:{fontSize:12},
			left:0,
			height:'auto',
			borderWidth:10
		});
		centreView.add(l);		
	}
	if (index.endTime!=undefined) {
		var l = Titanium.UI.createLabel({
			text:'End: '+index.endTime,
			textAlign:'left',
			font:{fontSize:12},
			left:0,
			height:'auto',
			borderWidth:10
		});
		centreView.add(l);		
	}
	tableRow.add(centreView);
	tableRow.hasChild = false;
	return tableRow;
}

//create a table view for an index top-level
function get_index_table_row(index, tableRow) {
	if (tableRow==undefined || tableRow==null)
		tableRow = Titanium.UI.createTableViewRow({height:'auto'});
	if (index.imageUrl!=undefined) {
		var imageView = Titanium.UI.createImageView({
			url:index.imageUrl,
			width:50,
			height:50,
			canScale:true,
			enableZoomControls:false,
			left:10,
			top:10,
			borderWidth:10
		});
		tableRow.add(imageView);
	}
	if (index.link!=undefined) {
		var link = Titanium.UI.createButton({
			title:'Web',
			width:'auto',
			height:'auto',
			borderWidth:10,
			left:70,
			top:10
		});
		tableRow.add(link);
		link.addEventListener('click',function(e) {
			open_browser(index.link);
		});
	}
	var centreView = Titanium.UI.createView({
		left:120,
		top:10,
		height:'auto',
		layout:'vertical'
	});
	var titleView = Titanium.UI.createView({		
		left:0,
		top:0,
		height:'auto'
	});
	var title = Titanium.UI.createLabel({
		text:index.title,
		textAlign:'left',
		font:{fontSize:16},
		left:0,
		height:'auto'
	});
	titleView.add(title);
	centreView.add(titleView);
	if (index.description!=undefined) {
		var description = Titanium.UI.createLabel({
			text:index.description,
			textAlign:'left',
			font:{fontSize:12},
			left:0,
			height:'auto',
			bottom:10,
			borderWidth:10
		});
		centreView.add(description);		
	}
	tableRow.add(centreView);
	tableRow.hasChild = false;
	return tableRow;
}