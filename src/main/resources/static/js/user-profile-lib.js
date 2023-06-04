function viewProfile(idEnc) {
	history.pushState(null, null, '/profile/view/' + idEnc);
	let url = '/profile/view/modal/' + idEnc;
	$("#user-profile-container").load(url, function() {
		setTimeout(function() { openModal("user-profile-modal") }, 1);
	});
}

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
		url : "/user/report/" + idEncoded,
		contentType : "text/plain",
		data: $("#report-comment").val(),
		success : function() {
			alert(getText("userprofile.js.success.report-user"));
			location.reload(true);
		},
		error : function(e) {
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

function verifyModal() {
    let userProfileModalActive = false;
    let userProfileModal = document.getElementById("user-profile-modal");
    if(userProfileModal) {
        userProfileModalActive = userProfileModal.classList.contains("is-active");
    }
    console.log(userProfileModalActive)
	if (userProfileModalActive) {
		closeModal("user-profile-modal");
		history.back();
        setTimeout(function() {openModal("verification-modal")}, 400);
	} else {
	    openModal("verification-modal");
	}
}

function upvoteVerification(idEnc) {
    closeModal("verification-modal");
	$.ajax({
		type : "POST",
		url : "/user/update/verification-picture/upvote/" + idEnc,
		success : function() {
            alert(getText("profile.verification.vote.success"));
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

function downvoteVerification(idEnc) {
    closeModal("verification-modal")
	$.ajax({
		type : "POST",
		url : "/user/update/verification-picture/downvote/" + idEnc,
		success : function() {
		    alert(getText("profile.verification.vote.success"));
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}