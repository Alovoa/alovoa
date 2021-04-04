var locationFound = false;

$(function() {
	bulmaSlider.attach();
});

function search() {
	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(function(position) {
		
			let distance = $("#max-distance-slider").val();
			let sort = $("#sort").val();
			let url = "/search/users/" + position.coords.latitude + "/"
					+ position.coords.longitude + "/" + distance + "/" + sort;
				
			$( ".loader-parent" ).css("display", "flex");
			$("#main-container").load(url, function() {
				
				var sliders = [];

				$('.swiper-container').each(function(index, element){
				    $(this).addClass('s'+index);
				    let slider = new Swiper('.s'+index, { initialSlide : 1,
						shortSwipes : false});
			    	

					slider.on('transitionEnd', function () { 
						let id = $(slider.el).attr("id");
						
						if(slider.activeIndex == 0) {
							likeUser(id);
						} else if(slider.activeIndex == 2) {
							hideUser(id);
						}
					 });
					 
					 sliders.push(slider);
				});
				
				$( ".loader-parent" ).css("display", "none");
				
			});
			$("#filter-div").addClass("searched");
			$("#search-div").addClass("searched");
		}, function(e) {
			console.log(e);
			alert(getText("search.js.error.no-location"));
		});
	} else {
		alert(getText("search.js.error.no-geolocation"));
	}
}

function likeUser(idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/like/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			hideProfileTile(idEnc);
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
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			hideProfileTile(idEnc);
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

var cardContentVisible = false;
function toggleCardContent() {
	
	let width = document.documentElement.clientWidth;
	
	if(cardContentVisible) {
		$(".content-background").removeClass("hidden");
		$(".profile-bottom").addClass("dimmed");
	} else {
		if(width < 1024) {
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
	});
}

function getUserDivFromButton(btn) {
	return $(btn).parent().parent().parent().parent();
}

function onDonateModalClicked() {
	$('#donate-modal').removeClass('is-active'); 
	//window.open('/donate-list', '_blank');
	loadIframe("donate-list" +  "?showHeader=false");
}

function playPauseAudio(userIdEnc) {
	let audio = document.getElementById("audio");
	console.log(audio.paused)
	if(!audio.paused) {
		audio.pause();
	} else {
		//showLoader();
		$.ajax({
		type : "GET",
		url : "/user/get/audio/" + userIdEnc ,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function(res) {
			if(res) {
				audio.src = res;
				audio.load();
				audio.play();
				//hideLoader();
			}
		},
		error : function(e) {
			console.log(e);
			//hideLoader();
			alert(getGenericErrorText());
		}
	});
	}
}
