var locationFound = false;
var map;
var popup;
var lat;
var long;

$(function() {
	bulmaSlider.attach();

	lat = 52.5;
	lon = 13.5;
	map = L.map('map').setView({ lon: lon, lat: lat }, 4);

	// add the OpenStreetMap tiles
	L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
		maxZoom: 8,
		attribution: '<a href="https://openstreetmap.org/copyright">OpenStreetMap contributors</a>'
	}).addTo(map);
	L.control.scale().addTo(map);
	popup = L.popup();
	map.on('click', onMapClick);
});

window.addEventListener('resize', () => {
	map.invalidateSize(true);
})


function onMapClick(e) {
	popup
		.setLatLng(e.latlng)
		.setContent('<input style="display: none;" id="map-lat" value="' + e.latlng.lat +
			'"><input style="display: none;" id="map-lon" value="' + e.latlng.lng +
			'"><button id="map-search-btn" class="button colored is-rounded is-primary" style="height: 56px;" onclick="mapSearchButtonClicked()"><i class="fa fa-search"></i></button>')
		.openOn(map);
}

function mapSearchButtonClicked() {
	lat = $("#map-lat").val();
	lon = $("#map-lon").val();
	searchBase();
}

function search() {
	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(function(position) {
			lat = position.coords.latitude;
			lon = position.coords.longitude;
			searchBase();
		}, function(e) {
			//console.log(e);	
			openModal("map-modal");
			map.invalidateSize(true);
			alert(getText("search.js.error.no-location"));
		});
	} else {
		//alert(getText("search.js.error.no-geolocation"));
		openModal("map-modal");
		map.invalidateSize(true);
	}
}

function searchBase() {
	let distance = $("#max-distance-slider").val();
	let sort = $("#sort").val();
	let url = "/search/users/" + lat + "/"
		+ lon + "/" + distance + "/" + sort;

	$(".loader-parent").css("display", "flex");
	$("#main-container").load(url, function() {

		closeModal();

		var sliders = [];

		$('.swiper-container').each(function(index, element) {
			$(this).addClass('s' + index);
			let slider = new Swiper('.s' + index, {
				initialSlide: 1,
				shortSwipes: false
			});


			slider.on('transitionEnd', function() {
				let id = $(slider.el).attr("id");

				if (slider.activeIndex == 0) {
					likeUser(id);
				} else if (slider.activeIndex == 2) {
					hideUser(id);
				}
			});

			sliders.push(slider);
		});

		$(".loader-parent").css("display", "none");

	});
	$("#filter-div").addClass("searched");
	$("#search-div").addClass("searched");
}

function likeUser(idEnc) {
	$.ajax({
		type: "POST",
		url: "/user/like/" + idEnc,
		headers: {
			"X-CSRF-TOKEN": $("input[name='_csrf']").val()
		},
		success: function() {
			hideProfileTile(idEnc);
		},
		error: function(e) {
			console.log(e);
			hideProfileTile(idEnc);
			//alert(getGenericErrorText());
		}
	});

}

function hideUser(idEnc) {
	$.ajax({
		type: "POST",
		url: "/user/hide/" + idEnc,
		headers: {
			"X-CSRF-TOKEN": $("input[name='_csrf']").val()
		},
		success: function() {
			hideProfileTile(idEnc);
		},
		error: function(e) {
			console.log(e);
			hideProfileTile(idEnc);
			//alert(getGenericErrorText());
		}
	});
}

var cardContentVisible = false;
function toggleCardContent() {

	let width = document.documentElement.clientWidth;

	if (cardContentVisible) {
		$(".content-background").removeClass("hidden");
		$(".profile-bottom").addClass("dimmed");
	} else {
		if (width < 1024) {
			$(".content-background").addClass("hidden");
			$(".profile-bottom").removeClass("dimmed");
		}
	}
	cardContentVisible = !cardContentVisible;
}

function hideProfileTile(id) {
	let tile = $("#" + id);
	$(tile).fadeOut(200, function() {
		tile.hide();
		searchAgain();
	});
}

function getUserDivFromButton(btn) {
	return $(btn).parent().parent().parent().parent();
}

function onDonateModalClicked() {
	$('#donate-modal').removeClass('is-active');
	window.open('/donate-list', '_blank');
}

function playPauseAudio(userIdEnc) {
	let audio = document.getElementById("audio");
	console.log(audio.paused)
	if (!audio.paused) {
		audio.pause();
	} else {
		//showLoader();
		$.ajax({
			type: "GET",
			url: "/user/get/audio/" + userIdEnc,
			headers: {
				"X-CSRF-TOKEN": $("input[name='_csrf']").val()
			},
			success: function(res) {
				if (res) {
					audio.src = res;
					audio.load();
					audio.play();
					//hideLoader();
				}
			},
			error: function(e) {
				console.log(e);
				//hideLoader();
				alert(getGenericErrorText());
			}
		});
	}
}

function viewProfile(idEnc) {
	let url = 'profile/view/' + idEnc;
	window.open(url, '_blank').focus();
}

function searchAgain() {
	if(!hasVisibleUsers()) {
		searchBase();
	}
}

function hasVisibleUsers() {
	let hasUsers = false;
	$(".user-search-card").each(function(i, obj) {
		if(!hasUsers && $(obj).is(":visible")) {
			hasUsers = true;
		}
	});
	return hasUsers;
}
