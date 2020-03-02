var locationFound = false;

function likeUser(idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/like/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			location.reload();
		},
		error : function(e) {
			// TODO
			console.log(e);
		}
	});

}

function hideUser(idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/hide/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			location.reload();
		},
		error : function(e) {
			// TODO
			console.log(e);
		}
	});
}

function blockUser(idEnc) {

	// TODO
	var r = confirm("Block user?");
	if (r == true) {

		$.ajax({
			type : "POST",
			url : "/user/block/" + idEnc,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			success : function() {
				location.reload();
			},
			error : function(e) {
				// TODO
				console.log(e);
			}
		});
	}
}

function unblockUser(idEnc) {

	// TODO
	var r = confirm("Unblock user?");
	if (r == true) {
		$.ajax({
			type : "POST",
			url : "/user/unblock/" + idEnc,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			success : function() {
				location.reload();
			},
			error : function(e) {
				// TODO
				console.log(e);
			}
		});
	}
}

function reportUser() {
	openModal("report-user-modal");
}

function reportUserSubmit() {
	$.ajax({
		type : "POST",
		url : "/user/report/" +  $("#id-enc").val() + "/" + $("#captcha-id").val() + "/" + $("#captcha").val() ,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			//TODO
			alert("User has been successfully reported!")
			location.reload();
		},
		error : function(e) {
			refreshCaptcha();
		}
	});
}