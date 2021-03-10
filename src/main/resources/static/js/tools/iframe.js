function loadIframe(url) {
	$("#iframe-div").show();
	$("#iframe-parent").append('<iframe id="iframe"></iframe>');
	$("#iframe").attr("src", url);
}

function deleteIframe() {
	$("#iframe-div").hide();
	$("#iframe-parent").empty();
}

function viewProfile(idEnc) {
	let url = 'profile/view/' + idEnc + "?showHeader=false";
	loadIframe(url);
}