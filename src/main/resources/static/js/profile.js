//TODO
const descriptionMaxLength = 255;

$(function() {

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
			$.ajax({
				type : "POST",
				url : "/user/update/description",
				headers : {
					"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
				},
				contentType : "text/plain",
				data : data,
				error : function(e) {
					// TODO
					console.log(e);
				}
			});
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
				error : function(e) {
					// TODO
					console.log(e);
				}
			});
		}
		
	});

});

function checkPassword() {
	var password = $("#password").val();
	var passwordRepeat = $("#password-repeat").val();

	if (password != passwordRepeat) {
		// TODO
		$("#password-info").html("Passwords do not match!");
		$("#password-info").show();
		return false;
	} else {
		if (isPasswordSecure(password)) {
			$("#password-info").hide();
			return true;
		} else {
			// TODO
			$("#password-info")
					.html(
							"Your password needs to be at least 7 characters long and must contain characters as well as numbers.");
			$("#password-info").show();
			return false;
		}
	}
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
