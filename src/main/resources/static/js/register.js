//TODO
const minAge = 16;
const maxAge = 99;

$(function() {

	let today = new Date();
	let startDate = new Date(today.setFullYear(today.getFullYear() - minAge)).toISOString().split('T')[0];
	let endDate = new Date(today.setFullYear(today.getFullYear() - maxAge)).toISOString().split('T')[0];
	
	let dobInput = $("#dob-input");
	dobInput.val(startDate);
	dobInput.attr('max', startDate); 
	dobInput.attr('min', endDate); 

	bulmaCollapsible.attach();

	$("#register-form").submit(
		function(e) {
			e.preventDefault();

			var actionUrl = e.currentTarget.action;
			let formdata = $("#register-form").serializeArray().reduce(
				function(a, x) {
					a[x.name] = x.value;
					return a;
				}, {});
			if (!checkPassword()) {
				return;
			}

			$.ajax({
				url: actionUrl,
				headers: {
					"X-CSRF-TOKEN": $("input[name='_csrf']").val()
				},
				type: 'POST',
				data: JSON.stringify(formdata),
				contentType: "application/json",
				success: function() {
					window.location = "/?confirm-registration";
				},
				error: function(e) {
					refreshCaptcha();
					console.log(e);
					alert(e.responseText);
				}
			});

		});
});
