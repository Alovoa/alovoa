$(function() {

	refreshCaptcha();
	let url = window.location.href;

	// TODO
	if (url.includes("?error")) {
		alert("An error occured while trying to reset your password. Please try again.")
	}

	$("#form").submit(function(e) {
		e.preventDefault();
		let captchaId = $("#captcha-id").val();
		let captchaText = $("#captcha").val();
		let email = $("#email").val();
		let url = "/password/reset";

		let data = {};
		data.captchaId = captchaId;
		data.captchaText = captchaText;
		data.email = email;

		$.ajax({
			type : "POST",
			url : url,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			data : JSON.stringify(data),
			contentType : "application/json",
			success : function() {
				location.href = "/?password-reset-requested";
			},
			error : function(e) {
				location.href = "/password/reset?error";
			}
		});

	});
});