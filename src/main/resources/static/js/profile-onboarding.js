//TODO
const maxImageSize = 600;

document.addEventListener("DOMContentLoaded", function(event) {
	var swiper = new Swiper(".onboarding-swiper", {
		direction: "vertical",
		simulateTouch: false,
		slidesPerView: 1,
		spaceBetween: 0,
		mousewheel: true,
		pagination: {
			el: ".swiper-pagination",
			clickable: true,
		},
	});
});