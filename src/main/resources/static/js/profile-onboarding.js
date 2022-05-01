//TODO
const maxImageSize = 600;

var timerDescription;
var timeoutDescription = 500;
var description;
var genderSwitches;
var intentionSelected = false;
var profilePicUploaded = false;
var submit = document.getElementById("submit");

document.addEventListener("DOMContentLoaded", function(event) {
	
	$.ajax({
		type: "POST",
		url: "/user/post",
		headers: {
			"X-CSRF-TOKEN": $("input[name='_csrf']").val()
		},
		error: function(e) {
			if (e.status == 403) {
				location.reload();
			}
		}
	});

	description = document.getElementById("description");
	genderSwitches = document.getElementsByClassName("gender-switch");

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

	let interest1 = $('#interest1');
	interest1.on('keyup paste', function() {
		interest1.val(getCleanInterest(interest1.val()));
	});
	
	let interest2 = $('#interest2');
	interest2.on('keyup paste', function() {
		interest2.val(getCleanInterest(interest2.val()));
	});
	
	let interest3 = $('#interest3');
	interest3.on('keyup paste', function() {
		interest3.val(getCleanInterest(interest3.val()));
	});

	let profilePicInput = $("#profilePictureUpload");
	profilePicInput.change(function() {
		readURL(profilePicInput[0]);
	});

	$('input:radio').change(function() {
		if (!intentionSelected) {
			intentionSelected = true;
			updateData();
		}
	});

	$('input:checkbox').change(function() {
		updateData();
	});

	//don't update at every keystroke
	$("#description").on('keyup paste', function() {
		if (timerDescription) {
			clearTimeout(timerDescription);
		}
		timerDescription = setTimeout(function() {
			updateData();
		}, timeoutDescription);
	});
	
	$('#submit').click(function(event) {
		subscribe(function() {
			showLoader();
			let data = {};
			data.intention = document.querySelector('input[name="intention"]:checked').value;
			data.preferredGenders = [];
			
			$('input:checkbox').each(function(index) {
				if ($(this).is(":checked")) {
					data.preferredGenders.push($(this).val());
				}
			});
			
			data.description = description.value;
			data.profilePicture = document.getElementById("profilePictureImg").src;
			data.interests = [];
			let interest1 = document.getElementById('interest1');
			let interest2 = document.getElementById('interest2');
			let interest3 = document.getElementById('interest3');
			if(interest1.value) {
				data.interests.push(interest1.value);
			}
			if(interest2.value) {
				data.interests.push(interest2.value);
			}
			if(interest3.value) {
				data.interests.push(interest3.value);
			}
			
			console.log(data)
			
			$.ajax({
				type: "POST",
				url: "/user/onboarding",
				headers: {
					"X-CSRF-TOKEN": $("input[name='_csrf']").val()
				},
				contentType: "application/json",
				data: JSON.stringify(data),
				success: function(e) {
					window.location = "/search";
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
					hideLoader();
					if (e.status == 403) {
						location.reload();
					}
				}
			});
		});
	});
});

function updateProfilePic() {
	$("#profilePictureUpload").click();
}

function getCleanInterest(userInput) {
	return userInput.replace(/[^a-zA-Z0-9-]/g, '').toLowerCase();
}

function interestInfo() {
	let text = getText("profile.interest.info");
	alert(text);
}

function updateData() {
	return new Promise(r => {
		let genderChecked = false;
		for (var i = 0; i < genderSwitches.length && !genderChecked; i++) {
			if (genderSwitches[i].checked) {
				genderChecked = true;
			}
		}

		if (description.value && profilePicUploaded && intentionSelected && genderChecked) {
			submit.disabled = false;
		} else {
			submit.disabled = true;
		}
	});
}

function readURL(input) {
	console.log(input)
	if (input.files && input.files[0]) {
		var reader = new FileReader();
		reader.onload = function(e) {
			getResizedImage(e.target.result, function(b64) {
				let img = $('#profilePictureImg');
				img.attr('src', b64);
				img.show();
				$('#profilePicture').hide();

				if (!profilePicUploaded) {
					updateData();
					profilePicUploaded = true;
				}
			});
		};
		reader.readAsDataURL(input.files[0]);
	}
}

function getResizedImage(b64, callback) {

	if(window.HTMLCanvasElement && window.CanvasRenderingContext2D) {
		var img = new Image();
		img.onload = function() {
			let canvas = document.createElement('canvas');
			let width = img.width;
			let height = img.height;
			
			let sx = 0;
			let sy = 0;
			
			if (width > height) {
				sx = width/2 - height/2;
				width = height;
			} else {
				sy = height/2 - width/2;
				height = width;
			}
			
			canvas.height = maxImageSize;
			canvas.width = maxImageSize;
			
			canvas.getContext('2d').drawImage(img, 
				sx, sy, width, height, 
				0, 0, maxImageSize, maxImageSize);
			
			var dataUrl = canvas.toDataURL('image/jpeg');
			return callback(dataUrl);
		}
		img.src = b64;
	} else {
		getBase64(file, callback);
	}
}

function getBase64InMB(base64) {
	return (base64.length * (3 / 4) - 1) / 1000000;
}

function getBase64(file, callback) {
	var reader = new FileReader();
	reader.readAsDataURL(file);
	reader.onload = function() {
		callback(reader.result);
	};
	reader.onerror = function() {
	};
}

