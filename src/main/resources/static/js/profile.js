//TODO
const descriptionMaxLength = 200;
const maxImageSize = 600;
const maxAudioSeconds = 10;

$(function() {

	//$(window).scrollTop(0);
	bulmaSlider.attach();
	bulmaCollapsible.attach();

	var mediaMaxSize = $("#mediaMaxSize").val();

	var swiper = new Swiper('.swiper-container', {
		centeredSlides: true,
		navigation: {
			nextEl: '.swiper-button-next',
			prevEl: '.swiper-button-prev',
		},
	});

	updateProfileWarning(true);

	$("#profilePicture").click(function(e) {
		$("#profilePictureUpload").click();
	});

	$("#addImageDiv").click(function(e) {
		$("#addImageInput").click();
	});

	$("#profilePictureUpload").change(function() {
		showLoader();
		let file = document.querySelector('#profilePictureUpload').files[0];
//		if (file.size > mediaMaxSize) {
//			hideLoader();
//			alert(getText("error.media.max-size-exceeded"));
//			return;
//		}

		resizeImage(file, function(b64) {
			if (b64) {
				if (getBase64InMB(b64) > mediaMaxSize) {
					hideLoader();
					alert(getText("error.media.max-size-exceeded"));
					return;
				}

				$.ajax({
					type: "POST",
					url: "/user/update/profile-picture",
					headers: {
						"X-CSRF-TOKEN": $("input[name='_csrf']").val()
					},
					contentType: "text/plain",
					data: b64,
					success: function() {
						location.reload();
					},
					error: function(e) {
						console.log(e);
						hideLoader();
						alert(getGenericErrorText());
					}
				});
			}
		});
	});

	$("#addImageInput").change(function() {
		let file = document.querySelector('#addImageInput').files[0];
		showLoader();
		resizeImage(file, function(b64) {
			if (b64) {
				if (getBase64InMB(b64) > mediaMaxSize) {
					hideLoader();
					alert(getText("error.media.max-size-exceeded"));
					return;
				}
				$.ajax({
					type: "POST",
					url: "/user/image/add",
					headers: {
						"X-CSRF-TOKEN": $("input[name='_csrf']").val()
					},
					contentType: "text/plain",
					data: b64,
					success: function() {
						location.reload();
					},
					error: function(e) {
						console.log(e);
						hideLoader();
						alert(getGenericErrorText());
					}
				});
			}
		});
	});

	var timerDescription;
	var timeoutDescription = 500;

	$('#description').on('keyup paste', function() {

		let data = $('#description').val();
		var maxlength = descriptionMaxLength;
		var currentLength = $(this).val().length;

		if (currentLength >= maxlength) {
			alert(getGenericMaxCharsErrorText());
			$('#description').val(data.substring(0, maxlength));
		} else {

			if (timerDescription) {
				clearTimeout(timerDescription);
			}
			if ($('#description').val) {
				timerDescription = setTimeout(function() {
					$.ajax({
						type: "POST",
						url: "/user/update/description",
						headers: {
							"X-CSRF-TOKEN": $("input[name='_csrf']").val()
						},
						contentType: "text/plain",
						data: data,
						success: function(e) {
							updateProfileWarning();
						},
						error: function(e) {
							console.log(e);
							alert(getGenericErrorText());
						}
					});
				}, timeoutDescription);
			}
		}
	});

	$("#intention").change(function(e) {

		let data = $("#intention").val();
		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/intention/" + data,
				headers: {
					"X-CSRF-TOKEN": $("input[name='_csrf']").val()
				},
				success: function(e) {
					//updateProfileWarning();
					location.reload();
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}
	});

	$("#min-age-slider").change(function(e) {

		let data = $("#min-age-slider").val();
		let dataMax = $("#max-age-slider").val();
		if (data > dataMax) {
			data = dataMax;
			$("#min-age-slider").val(data);
			$("#min-age-slider-output").val(data);
		}

		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/min-age/" + data,
				headers: {
					"X-CSRF-TOKEN": $("input[name='_csrf']").val()
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}
	});

	$("#max-age-slider").change(function(e) {

		let data = $("#max-age-slider").val();
		let dataMin = $("#min-age-slider").val();
		if (data < dataMin) {
			data = dataMin;
			$("#max-age-slider").val(data);
			$("#max-age-slider-output").val(data);
		}

		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/max-age/" + data,
				headers: {
					"X-CSRF-TOKEN": $("input[name='_csrf']").val()
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}
	});

	$(".gender-switch").change(function(e) {

		let obj = e.target;
		let checked = obj.checked;
		if (checked) {
			checked = 1;
		} else {
			checked = 0;
		}
		let data = $(obj).val();

		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/preferedGender/" + data + "/" + checked,
				headers: {
					"X-CSRF-TOKEN": $("input[name='_csrf']").val()
				},
				success: function() {
					updateProfileWarning();
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}

	});

	$("#interest-form").submit(function(e) {
		e.preventDefault();
		let val = e.target.elements['value'].value;

		$.ajax({
			url: "/user/interest/add/" + val,
			headers: {
				"X-CSRF-TOKEN": $("input[name='_csrf']").val()
			},
			type: 'POST',
			success: function() {
				location.reload();
			},
			error: function() {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	});

	$("#userdata-submit").click(function(e) {
		let url = "/user/userdata";
		window.open(url);
	});

	$("#delete-acc-submit").click(function(e) {

		$.ajax({
			type: "POST",
			url: "/user/delete-account/",
			contentType: "text/plain",
			headers: {
				"X-CSRF-TOKEN": $("input[name='_csrf']").val()
			},
			success: function(e) {
				alert(getText("profile.delete-account.success"));
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	});

	//AUDIO
	$("#audio-upload-button").click(function(e) {
		$("#audio-file").click();
	});

	$("#audio-file").change(function() {
		showLoader();
		let file = document.querySelector('#audio-file').files[0];
		resizeAudio(file, function(b64) {
			if (b64) {
				if (getBase64InMB(b64) > mediaMaxSize) {
					hideLoader();
					alert(getText("error.media.max-size-exceeded"));
					return;
				}
				
				var type = file.type.split('/')[1];

				$.ajax({
					type: "POST",
					url: "/user/update/audio/" + type,
					headers: {
						"X-CSRF-TOKEN": $("input[name='_csrf']").val()
					},
					contentType: "text/plain",
					data: b64,
					success: function() {
						location.reload();
					},
					error: function(e) {
						console.log(e);
						hideLoader();
						alert(getGenericErrorText());
					}
				});
			}
		});
	});
});

function deleteAudio() {
	if (confirm(getText("profile.audio.delete"))) {
		$.ajax({
			type: "POST",
			url: "/user/delete/audio",
			headers: {
				"X-CSRF-TOKEN": $("input[name='_csrf']").val()
			},
			success: function(e) {
				location.reload();
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	}
}

function deleteInterest(id) {
	$.ajax({
		type: "POST",
		url: "/user/interest/delete/" + id,
		headers: {
			"X-CSRF-TOKEN": $("input[name='_csrf']").val()
		},
		success: function() {
			location.reload();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

function deleteImage(id) {
	if (confirm(getText("profile.js.delete-image.confirm"))) {
		$.ajax({
			type: "POST",
			url: "/user/image/delete/" + id,
			headers: {
				"X-CSRF-TOKEN": $("input[name='_csrf']").val()
			},
			success: function(e) {
				location.reload();
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	}
}

function updateAccentColor(color) {
	$.ajax({
		type: "POST",
		url: "/user/accent-color/update/" + color,
		headers: {
			"X-CSRF-TOKEN": $("input[name='_csrf']").val()
		},
		success: function(e) {
			location.reload();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

function updateUiDesign() {
	let des = $("#ui-design-select").val();
	$.ajax({
		type: "POST",
		url: "/user/ui-design/update/" + des,
		headers: {
			"X-CSRF-TOKEN": $("input[name='_csrf']").val()
		},
		success: function(e) {
			location.reload();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}


function updateProfileWarning(onStart) {
	let url = "/profile/warning";
	let profileWarning = $("#profile-warning");
	profileWarning.load(url, function() {
		if (onStart) {
			profileWarning.hide();
			profileWarning.toggle("fast");
		} else {
			profileWarning.show();
		}
	});

}

function viewProfile(idEnc) {
	let url = 'profile/view/' + idEnc;
	window.open(url, '_blank').focus();
}

function resizeAudio(file, callback) {
	if(file.type == "audio/mpeg") {
		let cutter = new mp3cutter();
		cutter.cut(file, 0, maxAudioSeconds, function(cut) {
			getBase64(cut, callback);
		});
	} else if(file.type == "audio/x-wav"){
		getBase64(file, callback);
	} else {
		hideLoader();
		alert(getText("error.format-not-supported"));
	}
}

function resizeImage(file, callback) {
	var reader = new FileReader();
	reader.onload = function(readerEvent) {
		var img = new Image();
		img.onload = function() {
			let canvas = document.createElement('canvas');
			let width = img.width;
			let height = img.height;
			if (width > height) {
				if (width > maxImageSize) {
					height *= maxImageSize / width;
					width = maxImageSize;
				}
			} else {
				if (height > maxImageSize) {
					width *= maxImageSize / height;
					height = maxImageSize;
				}
			}
			canvas.width = width;
			canvas.height = height;
			canvas.getContext('2d').drawImage(img, 0, 0, width, height);
			var dataUrl = canvas.toDataURL('image/jpeg');
			return callback(dataUrl);
		}
		img.src = readerEvent.target.result;
	}
	reader.readAsDataURL(file);
}

function getBase64InMB(base64) {
	return (base64.length * (3/4) - 1) / 1000000;
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
