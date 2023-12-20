$(function() {
	mainContainerLoadCards("/search/users/default");
});

function searchBase(showLoader = true) {
	let distance = $("#max-distance-slider").val();
	let sort = $("#sort").val();
	let url = "/search/users/" + lat + "/"
		+ lon + "/" + distance + "/" + sort;
	mainContainerLoadCards(url, showLoader);

}

function mainContainerLoadCards(url, bShowLoader = true) {

    showLoader();
	$("#main-container").load(url, function() {

		closeModal();

		let shortSwipes = true;
		if(window.innerWidth <= 1024) {
			shortSwipes = true;
		} else {
			shortSwipes = false;
		}

		$('.swiper').each(function(index, element) {
			$(this).addClass('s' + index);
			let slider = new Swiper('.s' + index, {
				initialSlide: 1,
				shortSwipes: shortSwipes,
				simulateTouch: true
			});

			slider.on('transitionEnd', function() {
				let id = $(slider.el).attr("id");

				if (slider.activeIndex == 0) {
					likeUser(id);
				} else if (slider.activeIndex == 2) {
					hideUser(id);
				}
			});
		});

		let searchMessageDiv = $("#search-message");
		if (searchMessageDiv) {
			if (searchMessageDiv.text()) {
				alert(searchMessageDiv.text());
			}
		}

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
