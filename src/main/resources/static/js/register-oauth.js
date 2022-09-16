//TODO
const minAge = 16;
const maxAge = 99;

$(function() {

	let today = new Date();
	let startDate = new Date(today.setFullYear(today.getFullYear() - minAge)).toISOString().split('T')[0];
	let endDate = new Date(today.setFullYear(today.getFullYear() - maxAge)).toISOString().split('T')[0];

	let dobInput = $("#dob-input");
	//dobInput.val(startDate);
	dobInput.attr('max', startDate);
	dobInput.attr('min', endDate);

	const referrer = localStorage.getItem("referrer");
	if (referrer) {
		$("#referrer").val(referrer);
	}

	$("#register-form").submit(
		function(e) {
			e.preventDefault();

			var actionUrl = e.currentTarget.action;
			let formdata = $("#register-form").serializeArray().reduce(
				function(a, x) {
					a[x.name] = x.value;
					return a;
				}, {});

			$.ajax({
				url: actionUrl,
				type: 'POST',
				data: JSON.stringify(formdata),
				contentType: "application/json",
				success: function(data) {
					localStorage.removeItem("referrer");
					window.location = "/profile";
				},
				error: function(e) {
					console.log(e);
					alert(e.responseText);
				}
			});

		});
});
