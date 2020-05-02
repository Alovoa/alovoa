//TODO
const minPasswordLength = 7;

$("#password, #password-repeat").keyup(checkPassword);

function checkPassword() {
	var password = $("#password").val();
	var passwordRepeat = $("#password-repeat").val();

	if (password != passwordRepeat) {
		$("#password-info").html(getText("error.password-no-match"));
		$("#password-info").show();
		return false;
	} else {
		if (isPasswordSecure(password)) {
			$("#password-info").hide();
			return true;
		} else {
			$("#password-info")
					.html(getText("error.password-weak"));
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