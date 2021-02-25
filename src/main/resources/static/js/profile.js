//TODO
const descriptionMaxLength = 255;

$(function() {

	var swiper = new Swiper('.swiper-container', {
		centeredSlides: true,
		navigation : {
			nextEl : '.swiper-button-next',
			prevEl : '.swiper-button-prev',
		},
	});

	updateProfileWarning();

	$("#profilePicture").click(function(e) {
		$("#profilePictureUpload").click();
	});

	$("#addImageDiv").click(function(e) {
		$("#addImageInput").click();
	});

	$("#profilePictureUpload").change(function() {
		let file = document.querySelector('#profilePictureUpload').files[0];
		console.log($("input[name='_csrf']").val());
		getBase64(file, function(b64) {
			if (b64) {
				$.ajax({
					type : "POST",
					url : "/user/update/profile-picture",
					headers : {
						"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
					},
					contentType : "text/plain",
					data : b64,
					success : function() {
						location.reload(true);
					},
					error : function(e) {
						console.log(e);
						alert(getGenericErrorText());
					}
				});
			}
		});
	});

	$("#addImageInput").change(function() {
		let file = document.querySelector('#addImageInput').files[0];
		console.log($("input[name='_csrf']").val());
		getBase64(file, function(b64) {
			if (b64) {
				$.ajax({
					type : "POST",
					url : "/user/image/add",
					headers : {
						"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
					},
					contentType : "text/plain",
					data : b64,
					success : function() {
						location.reload(true);
					},
					error : function(e) {
						console.log(e);
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

					console.log("uploading description")
					$.ajax({
						type : "POST",
						url : "/user/update/description",
						headers : {
							"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
						},
						contentType : "text/plain",
						data : data,
						success : function(e) {
							updateProfileWarning();
						},
						error : function(e) {
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
				type : "POST",
				url : "/user/update/intention/" + data,
				headers : {
					"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
				},
				success : function(e) {
					updateProfileWarning();
				},
				error : function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}
	});

	$("#min-age-slider").change(function(e) {

		let data = $("#min-age-slider").val();
		console.log(data)

		if (data) {
			$("#min-age-display").html(data);
			$.ajax({
				type : "POST",
				url : "/user/update/min-age/" + data,
				headers : {
					"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
				},
				error : function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}
	});

	$("#max-age-slider").change(function(e) {

		let data = $("#max-age-slider").val();

		if (data) {
			$("#max-age-display").html(data);
			$.ajax({
				type : "POST",
				url : "/user/update/max-age/" + data,
				headers : {
					"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
				},
				error : function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}
	});

	$(".gender-switch").change(function(e) {

		let obj = e.target;
		console.log(obj);
		let checked = obj.checked;
		console.log(checked);
		if (checked) {
			checked = 1;
		} else {
			checked = 0;
		}
		let data = $(obj).val();

		if (data) {
			$.ajax({
				type : "POST",
				url : "/user/update/preferedGender/" + data + "/" + checked,
				headers : {
					"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
				},
				success : function(e) {
					updateProfileWarning();
				},
				error : function(e) {
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
			url : "/user/interest/add/" + val,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			type : 'POST',
			success : function(data) {
				location.reload(true);
			},
			error : function() {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	});

	$("#userdata-submit").click(function(e) {
		let url = "/user/userdata";
		window.open(url);
		/*
		$.ajax({
			type : "GET",
			url : url,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			success : function(e) {
				alert(getGenericSubmitSuccessText());
			},
			error : function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
		*/
	});

	$("#delete-acc-submit").click(function(e) {
	
		$.ajax({
			type : "POST",
			url : "/user/delete-account/",
			contentType : "text/plain",
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			success : function(e) {
				alert(getGenericSubmitSuccessText());
			},
			error : function(e) {
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
		let file = document.querySelector('#audio-file').files[0];
		getBase64(file, function(b64) {
			if (b64) {
			
			var type = file.type.split('/')[1];
			console.log(type);
			
				$.ajax({
					type : "POST",
					url : "/user/update/audio/" + type,
					headers : {
						"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
					},
					contentType : "text/plain",
					data : b64,
					success : function() {
						location.reload(true);
					},
					error : function(e) {
						console.log(e);
						alert(getGenericErrorText());
					}
				});
			}
		});
	});
	

});
 
 function deleteAudio() {
 	if(confirm(getText("profile.audio.delete"))) {
 		$.ajax({
		type : "POST",
		url : "/user/delete/audio",
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function(e) {
			location.reload(true);
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
 	}
 }

function deleteInterest(id) {
	$.ajax({
		type : "POST",
		url : "/user/interest/delete/" + id,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function(e) {
			location.reload(true);
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

function deleteImage(id) {
	if (confirm(getText("profile.js.delete-image.confirm"))) {
		$.ajax({
			type : "POST",
			url : "/user/image/delete/" + id,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			success : function(e) {
				location.reload(true);
			},
			error : function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	}
}

function updateProfileWarning() {
	let url = "/profile/warning";
	$("#profile-warning").load(url);
}

function getBase64(file, callback) {
	var reader = new FileReader();
	reader.readAsDataURL(file);
	reader.onload = function() {
		callback(reader.result);
	};
	reader.onerror = function(error) {
	};
}
