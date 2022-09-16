var locationFound = false;

$(function() {
	new Swiper('.swiper-container-user-profile', {
		navigation: {
			nextEl: '.swiper-button-next',
			prevEl: '.swiper-button-prev',
		},
		pagination: { el : '.swiper-pagination'}
	});
});

function downloadAndPlayAudio() {
	let userIdEnc = $("#user-id-enc").val();
	$.ajax({
		type : "GET",
		url : "/user/get/audio/" + userIdEnc,
		success : function(res) {
		 	let audio = document.getElementById('audio-profile');
		 	$("#audio-profile").show();
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

function blockUser(idEnc) {

	var r = confirm(getText("userprofile.js.block-user"));
	if (r == true) {

		$.ajax({
			type : "POST",
			url : "/user/block/" + idEnc,
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
	var element = document.getElementById("report-user-div");
	element.classList.toggle("display-none");
	element.scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'nearest' });
}

function reportUserSubmit(idEncoded) {
	$.ajax({
		type : "POST",
		url : "/user/report/" + idEncoded + "/"
				+ $("#captcha-id").val() + "/" + $("#captcha").val(),
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

function likeUser(idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/like/" + idEnc,
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
		success : function() {
			location.reload(true);
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

$(window).on("popstate", function(e) {
	if(!document.getElementById("user-profile-modal")) {
		location.reload();
	}
});