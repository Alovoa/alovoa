//TODO
const minPasswordLength = 7;

$("#password, #password-repeat").keyup(checkPassword);
var txtErrorNoMatch = "";
var txtErrorWeak = "";
$(function() {
	txtErrorNoMatch = getText("error.password-no-match");
	txtErrorWeak = getText("error.password-weak");
	console.log(txtErrorNoMatch)
	console.log(txtErrorWeak)
});

function checkPassword() {
	var password = $("#password").val();
	var passwordRepeat = $("#password-repeat").val();

	if (password != passwordRepeat) {
		$("#password-info").html(txtErrorNoMatch);
		$("#password-info").show();
		return false;
	} else {
		if (isPasswordSecure(password)) {
			$("#password-info").hide();
			return true;
		} else {
			$("#password-info")
					.html(txtErrorWeak);
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