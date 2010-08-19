// Common JS utilities for LobbyService web forms, etc.

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
	var value = $('select[name='+name+'] > option[selected]', form).attr('value');
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
//add a boolean-valued field to a JS Object using an checkbox 
function add_boolean_field(form, obj, name, input_name) {
	if (!input_name)
		input_name = name;
	var value = $(':input[name='+input_name+']',form).attr('checked');
	if (value!=null && value!='')
		obj[name] = Boolean(value);
}

// set a select option according to a field value
function set_select_from_field(form, data, name, input_name) {
	if (!input_name)
		input_name = name;
	$('select[name='+input_name+'] > option[selected]', form).removeAttr('selected');
	if (data[name]!=null && data[name]!='' && data[name]!=undefined)
		$('select[name='+input_name+'] > option[value='+data[name]+']', form).attr('selected','true');
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
