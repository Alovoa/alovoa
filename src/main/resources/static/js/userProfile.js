var locationFound = false;
var csrf = $("meta[name='_csrf']").attr("content");

$(function() {

	var swiper = new Swiper('.swiper-container', {
		navigation : {
			nextEl : '.swiper-button-next',
			prevEl : '.swiper-button-prev',
		},
	});
});

function downloadAndPlayAudio() {
	let userIdEnc = $("#user-id-enc").val();
	$.ajax({
		type : "GET",
		url : "/user/get/audio/" + userIdEnc ,
		headers : {
			"X-CSRF-TOKEN" : csrf
		},
		success : function(res) {
		 	let audio = document.getElementById('audio');
		 	$("#audio").show();
		 	$("#audio-play-button").hide();
			audio.src = res;
			audio.load();
			audio.play();
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});

}

function likeUser(idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/like/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : csrf
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
			"X-CSRF-TOKEN" : csrf
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
				"X-CSRF-TOKEN" : csrf
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

	var r = confirm(getText("userprofile.js.unblock-user"));
	if (r == true) {
		$.ajax({
			type : "POST",
			url : "/user/unblock/" + idEnc,
			headers : {
				"X-CSRF-TOKEN" : csrf
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

function reportUserSubmit(idEncoded) {
	$.ajax({
		type : "POST",
		url : "/user/report/" + idEncoded + "/"
				+ $("#captcha-id").val() + "/" + $("#captcha").val(),
		headers : {
			"X-CSRF-TOKEN" : csrf
		},
		contentType : "text/plain",
		data: $("#report-comment").val(),
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