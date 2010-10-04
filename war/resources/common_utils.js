// Common JS utilities for LobbyService web forms, etc.

//http://www.netlobo.com/url_query_string_javascript.html
function gup( name ) {  
	name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");  
	var regexS = "[\\?&]"+name+"=([^&#]*)";  
	var regex = new RegExp( regexS ); 
	var results = regex.exec( window.location.href ); 
	if( results == null )   
		return "";  
	else    
		return results[1];
}

//string to UTF-8 bytes in HEX for Hmac generation
// based on http://answers.oreilly.com/topic/657-how-to-use-utf-8-with-javascript/
function nibble_to_hex(nibble){
    var chars = '0123456789ABCDEF';
    return chars.charAt(nibble);
}
function escape_utf8(data) {
	if (data == '' || data == null){
		return '';
	}
	data = data.toString();
	var buffer = '';
	for(var i=0; i<data.length; i++){
		var c = data.charCodeAt(i);
		var bs = new Array();
		if (c > 0x10000){
			// 4 bytes
			bs[0] = 0xF0 | ((c & 0x1C0000) >>> 18);
			bs[1] = 0x80 | ((c & 0x3F000) >>> 12);
			bs[2] = 0x80 | ((c & 0xFC0) >>> 6);
			bs[3] = 0x80 | (c & 0x3F);
		}else if (c > 0x800){
			// 3 bytes
			bs[0] = 0xE0 | ((c & 0xF000) >>> 12);
			bs[1] = 0x80 | ((c & 0xFC0) >>> 6);
			bs[2] = 0x80 | (c & 0x3F);
		}else if (c > 0x80){
			// 2 bytes
			bs[0] = 0xC0 | ((c & 0x7C0) >>> 6);
			bs[1] = 0x80 | (c & 0x3F);
		}else{
			// 1 byte
			bs[0] = c;
		}
		for(var j=0; j<bs.length; j++){
			var b = bs[j];
			var hex = nibble_to_hex((b & 0xF0) >>> 4) 
			+ nibble_to_hex(b & 0x0F);
			buffer += hex;
		}
	}
	return buffer;
}
function escape_ascii(data) {
	if (data == '' || data == null){
		return '';
	}
	data = data.toString();
	var buffer = '';
	for(var i=0; i<data.length; i++){
		var b = data.charCodeAt(i) & 0xFF;
		var hex = nibble_to_hex((b & 0xF0) >>> 4) 
			+ nibble_to_hex(b & 0x0F);
			buffer += hex;
	}
	return buffer;
}

function currentTimeMillis() {
	var d = new Date();
	// I have seen some suggestions that Date.getTime() might be in the local timezone (which stinks)
	// but the specs certainly suggest it should be UTC, and that seems ok on my desktop and android phone
	// return d.getTime()+d.getTimezoneOffset() * 60000;
	return d.getTime();
}

function get_lobbyclient() {
	try {
		return lobbyclient;
	}
	catch (err) {}
	return undefined;
}

function log(msg) {
	if (get_lobbyclient()!=undefined)
		get_lobbyclient().log(msg);
// debug
	else
		alert(msg);
}


//===========================================================
// persistence

//fallback to non-persistent
var persistent_cache = {};
var persistence_type = undefined;
var key_prefix = 'game.js.';
var myLocalStorage;

function init_persistence() {
	if (window.localStorage!=undefined) {
		// W3C WebStorage
		myLocalStorage = window.localStorage;
		persistence_type = 'WebStorage';
	}
	else if (get_lobbyclient()!=undefined) {
		if (lobbyclient.getLocalStorage()!=undefined && lobbyclient.getLocalStorage()!=null) {
			myLocalStorage = {};
			// Java string is not a string?!
			myLocalStorage.getItem = function(key) { 
				var val = lobbyclient.getLocalStorage().getItem(key);
				if (val==null)
					return null;
				if (val==undefined)
					return undefined;
				return String(val);
			};
			myLocalStorage.setItem = function(key,value) { lobbyclient.getLocalStorage().setItem(key,value); };
			persistence_type = 'Lobbyclient';
		}
	}
	else {
		var cookies_ok = false;
		try {
			// 1 year
			// TODO fix: illegal format for expires: Fri Sep 16 2011 10:29:03 GMT+0000 (GMT)
			// -> Thu, 2 Aug 2001 20:47:11 UTC [~]
			document.cookie = key_prefix+'test=ok; expires='+timeToCookie(currentTimeMillis()+1000*60*60*24*365)+'; path=/browser/';
			cookies_ok = true;
		} catch (err) {
		}
		if (cookies_ok) {
			// try cookies
			myLocalStorage = {};
			myLocalStorage.getItem = function(key) {
				var val = get_cookie_value(key_prefix+key);
				if (val==null || val==undefined)
					return val;
				return decodeURIComponent(val);
			}
			myLocalStorage.setItem = function(key,value) {
				// 1 year
				// TODO fix: illegal format for expires: Fri Sep 16 2011 10:29:03 GMT+0000 (GMT)
				// -> Thu, 2 Aug 2001 20:47:11 UTC [~]
				document.cookie = key_prefix+key+'='+encodeURIComponent(value)+'; expires='+timeToCookie(currentTimeMillis()+1000*60*60*24*365)+'; path=/browser/';
				return;
			}
			persistence_type = 'Cookies';
		}
		else {
			// fallback to transient
			myLocalStorage = {};
			myLocalStorage.getItem = function(key) {
				return persistent_cache[key];
			}
			myLocalStorage.setItem = function(key, value) {
				persistent_cache[key] = value;
			}
		}
	}
}


// get a persistent value 
function set_persistent_string(key, value) {
	myLocalStorage.setItem(key, value);
}

function get_persistent_string(key) {
	return myLocalStorage.getItem(key);
}



//===========================================================
// Utilities for for building JSON requests from HTML Forms.

// add a string-valued field to a JS Object using an ordinary (e.g. text) input
function add_field(form, obj, name, input_name) {
	if (!input_name)
		input_name = name;
	var value = $(':input[name='+input_name+']',form).attr('value');
	if (value!=null && value!='')
		obj[name] = value;
}
//add a string-valued field to a JS Object using a select input
function add_select_field(form, obj, name, input_name) {
	if (!input_name)
    	input_name = name;
	var value = $('select[name='+name+']', form).attr('value');
	if (value!=null && value!='')
    	obj[name] = value;
}
//add a number-valued field to a JS Object using an ordinary (e.g. text) input
function add_int_field(form, obj, name, input_name) {
	if (!input_name)
		input_name = name;
	var value = $(':input[name='+input_name+']',form).attr('value');
	if (value!=null && value!='') {
		try {
			obj[name] = Number(value);
		}
		catch (err) {
			alert('Problem with '+name+' "'+value+'": '+err);
		}
	}
}
function add_double_field(form, obj, name, input_name) {
	add_int_field(form, obj, name, input_name);
}
//add a boolean-valued field to a JS Object using an checkbox 
function add_boolean_field(form, obj, name, input_name) {
	if (!input_name)
		input_name = name;
	var value = $(':input[name='+input_name+']',form).attr('checked');
	if (value!=null && value!='')
		obj[name] = true;
	else
		obj[name] = false;
}

// set a select option according to a field value
function set_select_from_field(form, data, name, input_name) {
	if (!input_name)
		input_name = name;
	var value = data[name];
	if (value==null || value==undefined)
		value = '';
	$('select[name='+input_name+']', form).attr('value', value);
}
//set an normal input according to a field value
function set_input_from_field(form, data, name, input_name) {
	if (!input_name)
		input_name = name;
	var value = data[name];
	if (value==null || value==undefined)
		value = '';
	$(':input[name='+input_name+']', form).attr('value',value);
}
//set an checkbox according to a (boolean) field value
function set_checkbox_from_field(form, data, name, input_name) {
	if (!input_name)
		input_name = name;
	var value = data[name];
	if (value!=null && value!=undefined)
		$(':input[name='+input_name+']',form).attr('checked',value);
}

//==================================================================
// Time utilities

// number leading 0 format helper
function format(text,len) {
	var str = String(text);
	while (str.length<2) 
    	str = '0'+str;
	return str;
}
// date to (relatively) pretty text format
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
//date to (relatively) pretty text format; local time zone
function prettyTimeToLocalString(time) {
	if (time==0)
		return "Unspecified";
	var date = new Date(time);
	var now = new Date();
	var str = '';
	if (date.getFullYear()==now.getFullYear() && date.getMonth()==now.getMonth() && date.getDate()==now.getDate())
		str =str + 'Today';
	else
		str = str + format(date.getFullYear(),4)+'-'+format(date.getMonth()+1,2)+'-'+format(date.getDate(),2);
	str = str +' '+format(date.getHours(),2)+':'+format(date.getMinutes(),2)+':'+format(date.getSeconds(),2);
	if (date.getTimezoneOffset()!=0) {
		var tzo = -date.getTimezoneOffset();
		str = str+' (UTC';
		if (tzo<0) {
			tzo = -tzo;
			str = str+'-';
		}
		else {
			str = str+'+';
		}
		var hours = Math.floor(tzo/60);
		str = str+hours;
		var minutes = tzo-60*hours;
		if (minutes!=0) {
			str = str+':'+format(minutes, 2);
		}
		str = str+')';
	}
	else 
		str = str +' (UTC)';
	return str;
}
// date to short text format
function timeToString(time) {
	var date = new Date(time);
	var str = format(date.getUTCFullYear(),4)+format(date.getUTCMonth()+1,2)+format(date.getUTCDate(),2)+'T'+
	format(date.getUTCHours(),2)+format(date.getUTCMinutes(),2)+format(date.getUTCSeconds(),2)+'Z';
	return str;
}
// short text format to date
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

function add_time_field(form, obj, name, input_name) {
	if (!input_name)
    	input_name = name;
	var value = $(':input[name='+input_name+']',form).attr('value');
	if (value!=null && value!='')
    	obj[name] = stringToTime(value);
}
//set an normal input according to a field value
function set_input_from_time_field(form, data, name, input_name) {
	if (!input_name)
		input_name = name;
	var value = data[name];
	if (value==null || value==undefined)
		value = '';
	$(':input[name='+input_name+']', form).attr('value',timeToString(value));
}
