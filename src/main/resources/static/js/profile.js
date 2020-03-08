//TODO
const descriptionMaxLength = 255;

$(function() {

	updateProfileWarning();
	
	$("#profilePicture").click(function(e) {
		$("#profilePictureUpload").click();
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
						location.reload();
					},
					error : function(e) {
						// TODO
						console.log(e);
					}
				});
			}
		});
	});

	var timerDescription;
	var timeoutDescription = 500;
	
	$('#description').on('keyup paste', function() {

		let data = $('#description').val();
		// TODO
		var maxlength = descriptionMaxLength;
		var currentLength = $(this).val().length;

		if (currentLength >= maxlength) {
			// TODO
			alert("You have reached the maximum number of characters.");
			$('#description').val(data.substring(0, maxlength));
		} else {
			
			if(timerDescription) {
				clearTimeout(timerDescription);
			}
			if ($('#desctiprion').val) {
				timerDescription = setTimeout(function(){
					
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
							// TODO
							console.log(e);
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
					// TODO
					console.log(e);
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
					// TODO
					console.log(e);
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
					// TODO
					console.log(e);
				}
			});
		}
	});

	$(".gender-switch").change(function(e) {
		
		let obj = e.target;
		console.log(obj);
		let checked = obj.checked;
		console.log(checked);
		if(checked) {
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
					// TODO
					console.log(e);
				}
			});
		}
		
	});

});

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
