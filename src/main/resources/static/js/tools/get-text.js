function getText(param) {
	var res;
	let url = "/text/" + param;
	$.ajax({
		type : "GET",
		url : url,
		async : false, // to make it synchronous,
		success : function(e) {
			res = e;
		},
		error : function(e) {
			console.log(e);
		}
	});
	
	return res;
}

function getTextAsync(param, callback) {
	var res;
	let url = "/text/" + param;
	$.ajax({
		type : "GET",
		url : url,
		async : true,
		success : function(e) {
			res = e;
			callback(res);
		},
		error : function(e) {
			console.log(e);
			callback(res);
		}
	});
}

function getGenericErrorText() {
	return getText("error.generic");
}

function getGenericSubmitSuccessText() {
	return getText("success.submit.generic");
}

function getGenericMaxCharsErrorText() {
	return getText("error.max-chars.generic");
}
