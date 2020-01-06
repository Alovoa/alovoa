//TODO
const minPasswordLength = 7;
const minAge = 16;

$(function() {
	
	let today = new Date();
	let startDate = new Date(today.setFullYear(today.getFullYear()-minAge ));
	
	// Initialize all input of date type.
	bulmaCalendar.attach('[type="date"]', {
		showHeader: false,
		startDate: startDate,
		maxDate: startDate,
		showClearButton: false,
		showTodayButton: false,
		cancelLabel: '<i class="fa fa-times" style="width: 100%"></i>',
		type: 'date',
		dateFormat: 'YYYY-MM-DD'
	});

	// To access to bulmaCalendar instance of an element
	const element = document.querySelector('#my-element');
	if (element) {
		// bulmaCalendar instance is available as element.bulmaCalendar
		element.bulmaCalendar.on('select', datepicker => {
			console.log(datepicker.data.value());
		});
	}

	$("#password, #password-repeat").keyup(checkPassword);

	$("#register-form").submit(function(e) {
		e.preventDefault();

		var actionUrl = e.currentTarget.action;
		let formdata = $("#register-form").serializeArray().reduce(function(a, x) { a[x.name] = x.value; return a; }, {});

		
		console.log(formdata);
		if (!checkPassword()) {
			return;
		}
		
		console.log(actionUrl)

		$.ajax({
			url : actionUrl,
			headers : {
				"X-CSRFToken" : $("input[name='_csrf']").val()
			},
			type : 'POST',
			data : JSON.stringify(formdata),
			contentType: "application/json",
			success : function(data) {
				window.location="/?confirm-registration";
			}, 
			error: function() {
				refreshCaptcha();
			}
		});

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
							"Your password needs to be at least 7 characters long and must contain characters and numbers.");
			$("#password-info").show();
			return false;
		}
	}
}

function isPasswordSecure(password) {
	if (password.length < minPasswordLength) {
		return false;
	} else if (password.match(/[a-z]/i) && password.match(/[0-9]+/)) {
		return true;
	} else {
		return false;
	}
}

function refreshCaptcha() {
	$.get("/captcha/generate", function(data) {
		console.log(data);
		let captcha = data;
		$("#captcha").val("");
		$("#captcha-id").val(captcha.id);
		$("#captcha-image").attr("src","data:image/png;base64," + captcha.image);	
	});
}
