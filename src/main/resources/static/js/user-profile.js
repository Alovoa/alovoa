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

$(window).on("popstate", function(e) {
	if(!document.getElementById("user-profile-modal")) {
		location.reload();
	}
});