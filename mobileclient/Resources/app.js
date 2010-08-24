// this sets the background color of the master UIView (when there are no windows/tab groups on it)
Titanium.UI.setBackgroundColor('#000');

Titanium.API.info("Starting mobileclient");

//
// create base UI tab and root window
//
var mainwin = Titanium.UI.createWindow({  
//    title:'Lobby Mobile Client'
});

var optiondata = [{title:'Game Index',winurl:'index.js'},
                  {title:'Current Games'}];

var table = Titanium.UI.createTableView({data:optiondata});

table.addEventListener('click', function(e){
	if (e.rowData.winurl) {
		var win = Titanium.UI.createWindow({url:e.rowData.winurl,title:e.rowData.title});
		win.open({modal:true});
	}
});

mainwin.add(table);

mainwin.open();

