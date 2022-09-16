$(function() {
	bulmaCollapsible.attach();
	$("#imprint-form").submit(
		function(e) {
			e.preventDefault();

			var actionUrl = e.currentTarget.action;
			let formdata = $("#imprint-form").serializeArray().reduce(
				function(a, x) {
					a[x.name] = x.value;
					return a;
				}, {});

			$.ajax({
				url: actionUrl,
				type: 'POST',
				data: JSON.stringify(formdata),
				contentType: "application/json",
				success: function(e) {
					document.getElementById("imprint-form").reset();
					alert(getText("success.generic"));
				},
				error: function(e) {
					refreshCaptcha();
					console.log(e);
					alert(e.responseText);
				}
			});

		});
});