var locationFound = false;

$(function() {

	var swiper = new Swiper('.swiper-container', {
		navigation : {
			nextEl : '.swiper-button-next',
			prevEl : '.swiper-button-prev',
		},
	});
});

function likeUser(idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/like/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			location.reload(true);
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
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
			location.reload(true);
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

function blockUser(idEnc) {

	var r = confirm(getText("userprofile.js.block-user"));
	if (r == true) {

		$.ajax({
			type : "POST",
			url : "/user/block/" + idEnc,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			success : function() {
				location.reload(true);
			},
			error : function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	}
}

function unblockUser(idEnc) {

	var r = confirm(getText("userprofile.js.block-user"));
	if (r == true) {
		$.ajax({
			type : "POST",
			url : "/user/unblock/" + idEnc,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			success : function() {
				location.reload(true);
			},
			error : function(e) {
				console.log(e);
				alert(getGenericErrorText());
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
		url : "/user/report/" + $("#id-enc").val() + "/"
				+ $("#captcha-id").val() + "/" + $("#captcha").val(),
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			alert(getText("userprofile.js.success.report-user"));
			location.reload(true);
		},
		error : function(e) {
			refreshCaptcha();
			alert(getGenericErrorText());
		}
	});
}