function getText(param) {
	var res;
	let url = "/text/" + param;
	console.log(url);
	$.ajax({
		type : "GET",
		url : url,
		async : false, // to make it synchronous,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
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
	console.log(url);
	$.ajax({
		type : "GET",
		url : url,
		async : true,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function(e) {
			res = e;
			callback(res);
		},
		error : function(e) {
			console.log(e);
			callback(res);
		}
	});
	
	return res;
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
