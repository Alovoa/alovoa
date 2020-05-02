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
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}
		
	});
	
	$("#userdata-submit").click(function(e) {

		let password = $("#userdata-password").val();
		
		if (password) {
			$.ajax({
				type : "POST",
				contentType : "text/plain",
				data: password,
				url : "/user/userdata/",
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
		}
	});
	
	$("#delete-acc-submit").click(function(e) {

		let password = $("#delete-acc-password").val();
		
		if (password) {
			$.ajax({
				type : "POST",
				contentType : "text/plain",
				data: password,
				url : "/user/delete-account/",
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
