$(function() {
	mainContainerLoadCards("/search/users/default");
});

function mainContainerLoadCards(url, bShowLoader = true) {
    showLoader();
	$("#main-container").load(url, function() {
		hideLoader();
	});
}

function hideProfileTile(id) {
	closeModal();
	let tile = $("#" + id);
	$(tile).fadeOut(100, function() {
		tile.hide();
		searchAgain();
	});
}

function getUserDivFromButton(btn) {
	return $(btn).parent().parent().parent().parent();
}

function hasVisibleUsers() {
	let hasUsers = false;
	$(".user-search-card").each(function(i, obj) {
		if (!hasUsers && $(obj).is(":visible")) {
			hasUsers = true;
		}
	});
	return hasUsers;
}

function viewProfile(idEnc) {
	let url = '/admin/profile/view/' + idEnc;
	window.open(url, '_blank').focus();
}

function viewProfileMedia(idEnc) {
	let url = '/admin/profile/view/' + idEnc + '/media';
	window.open(url, '_blank').focus();
}
