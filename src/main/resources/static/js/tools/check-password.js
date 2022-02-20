//TODO
const minPasswordLength = 7;

$("#password, #password-repeat").keyup(checkPassword);
var txtErrorNoMatch = "";
var txtErrorWeak = "";
$(function() {
	txtErrorNoMatch = getText("error.password-no-match");
	txtErrorWeak = getText("error.password-weak");
});

function checkPassword() {
	var password = $("#password").val();

	if (isPasswordSecure(password)) {
		$("#password-info").css("visibility", "hidden");
		return true;
	} else {
		$("#password-info")
				.html(txtErrorWeak);
		$("#password-info").css("visibility", "visible");
		return false;
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