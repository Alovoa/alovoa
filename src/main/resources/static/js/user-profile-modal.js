var csrf = $("input[name='_csrf']").val();

function viewProfile(idEnc) {
	history.pushState(null, null, 'profile/view/' + idEnc);
	let url = '/profile/view/modal/' + idEnc;
	$("#user-profile-container").load(url, function() {
		setTimeout(function() { openModal("user-profile-modal") }, 1);
	});
}

function blockUser(idEnc) {

	var r = confirm(getText("userprofile.js.block-user"));
	if (r == true) {

		$.ajax({
			type: "POST",
			url: "/user/block/" + idEnc,
			headers: {
				"X-CSRF-TOKEN": csrf
			},
			success: function() {
				alert(getText("success.generic"));
			},
			error: function(e) {
				console.log(e);
				console.log(csrf);
				alert(getGenericErrorText());
			}
		});
	}
}

function unblockUser(idEnc) {

	var r = confirm(getText("userprofile.js.unblock-user"));
	if (r == true) {
		$.ajax({
			type: "POST",
			url: "/user/unblock/" + idEnc,
			headers: {
				"X-CSRF-TOKEN": csrf
			},
			success: function() {
				//location.reload(true);
				alert(getText("success.generic"));
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	}
}

function reportUserSubmit(idEncoded) {
	$.ajax({
		type: "POST",
		url: "/user/report/" + idEncoded + "/"
			+ $("#captcha-id").val() + "/" + $("#captcha").val(),
		headers: {
			"X-CSRF-TOKEN": csrf
		},
		contentType: "text/plain",
		data: $("#report-comment").val(),
		success: function() {
			alert(getText("userprofile.js.success.report-user"));
			//location.reload(true);
		},
		error: function(e) {
			refreshCaptcha();
			alert(getGenericErrorText());
		}
	});
}

$(window).on("popstate", function(e) {
	closeModal();
});