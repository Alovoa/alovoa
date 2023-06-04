var locationFound = false;
var mymap;
var popup;
var lat;
var lon;

$(function() {
	
	getUpdates();
	lat = $("#lat").attr("value");
	lon = $("#lon").attr("value");
	
	bulmaSlider.attach();

	mymap = L.map('map').setView({ lon: 4.9, lat: 52.3 }, 4);

	// add the OpenStreetMap tiles
	L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
		maxZoom: 8,
		attribution: '<a href="https://openstreetmap.org/copyright">OpenStreetMap contributors</a>'
	}).addTo(mymap);
	L.control.scale().addTo(mymap);
	popup = L.popup();
	mymap.on('click', onMapClick);

	search(true);
});

window.addEventListener('resize', resize);

function resize() {
	mymap.invalidateSize(true);
}


function onMapClick(e) {
	popup
		.setLatLng(e.latlng)
		.setContent('<input style="display: none;" id="map-lat" value="' + e.latlng.lat +
			'"><input style="display: none;" id="map-lon" value="' + e.latlng.lng +
			'"><button id="map-search-btn" class="button colored is-rounded is-primary" style="height: 56px;" onclick="mapSearchButtonClicked()"><i class="fa fa-search"></i></button>')
		.openOn(mymap);
}

function mapSearchButtonClicked() {
	lat = $("#map-lat").val();
	lon = $("#map-lon").val();
	searchBase();
}

function search(defaultSearch) {

	
	if (defaultSearch && document.getElementById("has-location")) {
		mainContainerLoadCards("/search/users/default");
	} else {
		showLoader();
		if (navigator.geolocation) {
			navigator.geolocation.getCurrentPosition(function(position) {
				lat = position.coords.latitude;
				lon = position.coords.longitude;
				searchBase(false);
			}, function() {
				hideLoader();
				openModal("map-modal");
				mymap.invalidateSize(true);
				alert(getText("search.js.error.no-location"));
			});
		} else {
			hideLoader();
			alert(getText("search.js.error.no-geolocation"));
			openModal("map-modal");
			mymap.invalidateSize(true);
		}
	}
}

function searchBase(showLoader = true) {
	let distance = $("#max-distance-slider").val();
	let sort = $("#sort").val();
	let url = "/search/users/" + lat + "/"
		+ lon + "/" + distance + "/" + sort;
	mainContainerLoadCards(url, showLoader);

}

function mainContainerLoadCards(url, bShowLoader = true) {
	
	if(bShowLoader) {
		showLoader();
	}
	$("#main-container").load(url, function() {
		
		closeModal();
		
		showDonatePopup();
		
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

function searchSettingsClicked() {
	let searchModal = document.getElementById("search-settings-modal");
	let isActive = searchModal.classList.contains("is-active");
	if (isActive) {
		closeModal();
	} else {
		openModal("search-settings-modal")
	}
}

function likeUser(idEnc) {
	$.ajax({
		type: "POST",
		url: "/user/like/" + idEnc,
		success: function() {
			hideProfileTile(idEnc);
		},
		error: function(e) {
			console.log(e);
			hideProfileTile(idEnc);
			alert(getGenericErrorText());
			if (e.status == 403) {
				location.reload();
			}
		}
	});

}

function hideUser(idEnc) {
	$.ajax({
		type: "POST",
		url: "/user/hide/" + idEnc,
		success: function() {
			hideProfileTile(idEnc);
		},
		error: function(e) {
			console.log(e);
			hideProfileTile(idEnc);
			alert(getGenericErrorText());
			if (e.status == 403) {
				location.reload();
			}
		}
	});
}

function toggleCardContent() {
	$(".description").toggleClass("display-none");
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

function onDonateModalClicked() {
	closeModal("donate-modal");
	window.open('/donate-list', '_blank');
}

function playPauseAudio(userIdEnc) {
	let audio = document.getElementById("audio");
	if (!audio.paused) {
		audio.pause();
	} else {
		//showLoader();
		$.ajax({
			type: "GET",
			url: "/user/get/audio/" + userIdEnc,
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

function showDonatePopup() {
	let donationPopup = document.getElementById("show-donation-popup");
	if (donationPopup) {
		openModal('donate-modal');
		donationPopup.remove();
	}
}

function searchAgain() {
	if (!hasVisibleUsers()) {
		searchBase();
	}
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
