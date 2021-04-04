function loadIframe(url) {
	$("#iframe-div").show();
	$("#iframe-parent").append('<iframe id="iframe"></iframe>');
	$("#iframe").attr("src", url);
	showLoader();
	setTimeout(function() {
		hideLoader();
	}, 1000);
}

function deleteIframe() {
	$("#iframe-div").hide();
	$("#iframe-parent").empty();
}

function viewProfile(idEnc) {
	let url = 'profile/view/' + idEnc + "?showHeader=false";
	loadIframe(url);
}

function showLoader() {
	$(".loader-parent").css("display", "flex");
}

function hideLoader() {
	$(".loader-parent").css("display", "none");
}