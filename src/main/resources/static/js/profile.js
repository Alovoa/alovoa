//TODO
const descriptionMaxLength = 200;
const maxImageSize = 600;
const maxAudioSeconds = 10;
const twa_playstore = "android-app://com.alovoa.alovoa_playstore";

$(function() {
	
	getUpdates();
	//$(window).scrollTop(0);
	bulmaSlider.attach();
	bulmaCollapsible.attach();

	if (document.referrer.includes(twa_playstore)) {
		localStorage.setItem("twa_playstore", true);
	}

	var mediaMaxSize = $("#mediaMaxSize").val() / 1000000;

	var swiper = new Swiper('.swiper-container', {
		centeredSlides: true,
		navigation: {
			nextEl: '.swiper-button-next',
			prevEl: '.swiper-button-prev',
		},
		pagination: { el: '.swiper-pagination' }
	});

	if (!navigator.canShare) {
		$("#referral-share-btn").hide();
	}

	let interest = $('#interest');
	if(interest.length) {
		interest.on('keyup paste', function() {
			interest.val(getCleanInterest(interest.val()));
		});
		interest.autocomplete({
			minLength: 3,
			delay: 500,
			source: function(request, response) {
				$.getJSON("/user/interest/autocomplete/" + encodeURI(request.term), {}, response);
			},
			focus: function(event, ui) {
				interest.val(ui.item.name);
				return false;
			},
			select: function(event, ui) {
				interest.val(ui.item.name);
				$("#interest-form").submit();
				return false;
			}
		})
		.autocomplete("instance")._renderItem = function(ul, item) {
			return $("<li>")
				.append("<div>" + item.name + ' <span class="interest-autocomplete-count">(' + item.count + ")</span></div>")
				.appendTo(ul);
		};
	}

	//updateProfileWarning();

	$("#profilePicture").click(function(e) {
        //TODO check verificationStatus first
        openModal("profilepic-change-modal");
	});

	$("#addImageDiv").click(function(e) {
		$("#addImageInput").click();
	});

	$("#profilePictureUpload").change(function() {
		showLoader();
		let file = document.querySelector('#profilePictureUpload').files[0];

		resizeImage(file, function(b64) {
			if (b64) {
				if (getBase64InMB(b64) > mediaMaxSize) {
					hideLoader();
					alert(getText("error.media.max-size-exceeded"));
					return;
				}

				$.ajax({
					type: "POST",
					url: "/user/update/profile-picture",
					contentType: "text/plain",
					data: b64,
					success: function() {
						location.reload();
					},
					error: function(e) {
						console.log(e);
						hideLoader();
						alert(getGenericErrorText());
						if (e.status == 403) {
							location.reload();
						}
					}
				});
			}
		});
	});

	$("#addImageInput").change(function() {
		let file = document.querySelector('#addImageInput').files[0];
		showLoader();
		resizeImage(file, function(b64) {
			if (b64) {
				if (getBase64InMB(b64) > mediaMaxSize) {
					hideLoader();
					alert(getText("error.media.max-size-exceeded"));
					return;
				}
				$.ajax({
					type: "POST",
					url: "/user/image/add",
					contentType: "text/plain",
					data: b64,
					success: function() {
						location.reload();
					},
					error: function(e) {
						console.log(e);
						hideLoader();
						alert(getGenericErrorText());
						if (e.status == 403) {
							location.reload();
						}

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

			if (timerDescription) {
				clearTimeout(timerDescription);
			}
			if ($('#description').val) {
				timerDescription = setTimeout(function() {
					$.ajax({
						type: "POST",
						url: "/user/update/description",
						contentType: "text/plain",
						data: data,
						success: function(e) {
							updateProfileWarning();
						},
						error: function(e) {
							console.log(e);
							alert(getGenericErrorText());
							if (e.status == 403) {
								location.reload();
							}
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
				type: "POST",
				url: "/user/update/intention/" + data,
				success: function(e) {
					//updateProfileWarning();
					alert(getText("profile.warning.intention.limit"))
					//location.reload();
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
			});
		}
	});

	$("#min-age-slider").change(function(e) {

		let data = $("#min-age-slider").val();
		let dataMax = $("#max-age-slider").val();
		if (data > dataMax) {
			data = dataMax;
			$("#min-age-slider").val(data);
			$("#min-age-slider-output").val(data);
		}

		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/min-age/" + data,
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
					if (e.status == 403) {
						location.reload();
					}
				}
			});
		}
	});

	$("#max-age-slider").change(function(e) {

		let data = $("#max-age-slider").val();
		let dataMin = $("#min-age-slider").val();
		if (data < dataMin) {
			data = dataMin;
			$("#max-age-slider").val(data);
			$("#max-age-slider-output").val(data);
		}

		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/max-age/" + data,
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
					if (e.status == 403) {
						location.reload();
					}
				}
			});
		}
	});

	$(".gender-switch").change(function(e) {

		let obj = e.target;
		let checked = obj.checked;
		if (checked) {
			checked = 1;
		} else {
			checked = 0;
		}
		let data = $(obj).val();

		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/preferedGender/" + data + "/" + checked,
				success: function() {
					updateProfileWarning();
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
					if (e.status == 403) {
						location.reload();
					}
				}
			});
		}

	});

	$(".gender-switch").change(function(e) {

		let obj = e.target;
		let checked = obj.checked;
		if (checked) {
			checked = 1;
		} else {
			checked = 0;
		}
		let data = obj.value;

		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/preferedGender/" + data + "/" + checked,
				success: function() {
					updateProfileWarning();
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
					if (e.status == 403) {
						location.reload();
					}
				}
			});
		}

	});

	$(".misc-info-switch").change(function(e) {

		let obj = e.target;
		let checked = obj.checked;
		if (checked) {
			checked = 1;
		} else {
			checked = 0;
		}
		let data = obj.value;

		if (data) {
			$.ajax({
				type: "POST",
				url: "/user/update/misc-info/" + data + "/" + checked,
				success: function() {
					if (obj.classList.contains("misc-info-single")) {
						let parent = obj.parentNode.parentNode;
						let inputs = parent.getElementsByTagName('input');
						if (obj.checked) {
							for (let i in inputs) {
								if (inputs[i].value != obj.value) {
									inputs[i].checked = false;
								}
							}
						}
					}
				},
				error: function(e) {
					console.log(e);
					alert(getGenericErrorText());
					if (e.status == 403) {
						location.reload();
					}
				}
			});
		}

	});

	$("#interest-form").submit(function(e) {
		e.preventDefault();
		let val = e.target.elements['value'].value;

		$.ajax({
			url: "/user/interest/add/" + val,
			type: 'POST',
			success: function() {
				location.reload();
			},
			error: function() {
				console.log(e);
				alert(getGenericErrorText());
				if (e.status == 403) {
					location.reload();
				}
			}
		});
	});

	$("#password-change-submit").click(function(e) {
		let url = "/password/reset";
		$.ajax({
			type: "POST",
			url: url,
			data: "{}",
			contentType: "application/json",
			success: function(e) {
				alert(getText("index.js.password-reset-requested"));
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
				if (e.status == 403) {
					location.reload();
				}
			}
		});
	});

	$("#delete-acc-submit").click(function(e) {

		$.ajax({
			type: "POST",
			url: "/user/delete-account",
			success: function(e) {
				alert(getText("profile.delete-account.success"));
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
				if (e.status == 403) {
					location.reload();
				}
			}
		});
	});

	//AUDIO
	$("#audio-upload-button").click(function(e) {
		$("#audio-file").click();
	});

	$("#audio-file").change(function() {
		showLoader();
		let file = document.querySelector('#audio-file').files[0];
		resizeAudio(file, function(b64) {
			if (b64) {
				if (getBase64InMB(b64) > mediaMaxSize) {
					hideLoader();
					alert(getText("error.media.max-size-exceeded"));
					return;
				}

				var type = file.type.split('/')[1];

				$.ajax({
					type: "POST",
					url: "/user/update/audio/" + type,
					contentType: "text/plain",
					data: b64,
					success: function() {
						location.reload();
					},
					error: function(e) {
						console.log(e);
						hideLoader();
						alert(getGenericErrorText());
						if (e.status == 403) {
							location.reload();
						}
					}
				});
			}
		});
	});

	$("#submit-verification-button").click(function(e) {
        $("#verificationPictureUpload").click();
    });

    $("#verificationPictureUpload").change(function() {
        showLoader();
        let file = document.querySelector('#verificationPictureUpload').files[0];

        resizeImage(file, function(b64) {
            if (b64) {
                if (getBase64InMB(b64) > mediaMaxSize) {
                    hideLoader();
                    alert(getText("error.media.max-size-exceeded"));
                    return;
                }

                $.ajax({
                    type: "POST",
                    url: "/user/update/verification-picture",
                    contentType: "text/plain",
                    data: b64,
                    success: function() {
                        location.reload();
                    },
                    error: function(e) {
                        console.log(e);
                        hideLoader();
                        alert(getGenericErrorText());
                        if (e.status == 403) {
                            location.reload();
                        }
                    }
                });
            }
        });
    });
});

function copyClipboard(txt) {

	const successMsg = getText("success.clipboard");

	var m = document;
	txt = m.createTextNode(txt);
	var w = window;
	var b = m.body;
	b.appendChild(txt);
	if (b.createTextRange) {
		var d = b.createTextRange();
		d.moveToElementText(txt);
		d.select();
		m.execCommand('copy');
	}
	else {
		var d = m.createRange();
		var g = w.getSelection;
		d.selectNodeContents(txt);
		g().removeAllRanges();
		g().addRange(d);
		m.execCommand('copy');
		g().removeAllRanges();
	}
	txt.remove();

	alert(successMsg);
}

async function shareUrl(url) {
	const data = {
		title: 'Alovoa',
		text: 'Alovoa',
		url: url
	}
	try {
		await navigator.share(data)
	} catch (err) { }
}

function getCleanInterest(userInput) {
	return userInput.replace(/[^a-zA-Z0-9-]/g, '').toLowerCase();
}

function deleteAudio() {
	if (confirm(getText("profile.audio.delete"))) {
		$.ajax({
			type: "POST",
			url: "/user/delete/audio",
			success: function(e) {
				location.reload();
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
				if (e.status == 403) {
					location.reload();
				}
			}
		});
	}
}

function deleteInterest(val) {
	$.ajax({
		type: "POST",
		url: "/user/interest/delete/" + val,
		success: function() {
			location.reload();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
			if (e.status == 403) {
				location.reload();
			}
		}
	});
}

function interestInfo() {
	let text = getText("profile.interest.info");
	alert(text);
}

function audioInfo() {
	let text = getText("profile.audio.info");
	alert(text);
}

function referralInfo() {
	let text = document.getElementById("referralInfo").innerHTML;
	alert(text);
}

function deleteImage(id) {
	if (confirm(getText("profile.js.delete-image.confirm"))) {
		$.ajax({
			type: "POST",
			url: "/user/image/delete/" + id,
			success: function(e) {
				location.reload();
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
				if (e.status == 403) {
					location.reload();
				}
			}
		});
	}
}

function updateAccentColor(color) {
	$.ajax({
		type: "POST",
		url: "/user/accent-color/update/" + color,
		success: function(e) {
			location.reload();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
			if (e.status == 403) {
				location.reload();
			}
		}
	});
}

function updateUiDesign() {
	let des = $("#ui-design-select").val();
	$.ajax({
		type: "POST",
		url: "/user/ui-design/update/" + des,
		success: function(e) {
			location.reload();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
			if (e.status == 403) {
				location.reload();
			}
		}
	});
}

function updateShowZodiac() {
	let val = $("#show-zodiac-select").val();
	$.ajax({
		type: "POST",
		url: "/user/show-zodiac/update/" + val,
		success: function(e) {
			return false;
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
			if (e.status == 403) {
				location.reload();
			}
		}
	});
	return false;
}

function updateUnits() {
	let val = $("#ui-units-select").val();
	$.ajax({
		type: "POST",
		url: "/user/units/update/" + val,
		success: function(e) {
			return false;
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
			if (e.status == 403) {
				location.reload();
			}
		}
	});
	return false;
}


function updateProfileWarning() {
	let url = "/profile/warning";

	$.ajax({
		type: "GET",
		url: url,
		success: function(res) {
			let warning = "profile-warning-collapsible";
			if (!res.includes(warning)) {
				$("#" + warning).addClass("disabled");
			} else {
				$("#" + warning).removeClass("disabled");
			}
			warning = "no-profile-picture";
			if (!res.includes(warning)) {
				$("#" + warning).addClass("disabled");
			} else {
				$("#" + warning).removeClass("disabled");
			}
			warning = "no-description";
			if (!res.includes(warning)) {
				$("#" + warning).addClass("disabled");
			} else {
				$("#" + warning).removeClass("disabled");
			}
			warning = "no-intention";
			if (!res.includes(warning)) {
				$("#" + warning).addClass("disabled");
			} else {
				$("#" + warning).removeClass("disabled");
			}
			warning = "no-gender";
			if (!res.includes(warning)) {
				$("#" + warning).addClass("disabled");
			} else {
				$("#" + warning).removeClass("disabled");
			}
			warning = "no-location";
			if (!res.includes(warning)) {
				$("#" + warning).addClass("disabled");
			} else {
				$("#" + warning).removeClass("disabled");
			}
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
			if (e.status == 403) {
				location.reload();
			}
		}
	});

}

function getUserData(idEnc) {
	let url = "/user/userdata/" + idEnc;
	window.open(url);
}

function modalVerificationImage() {
    openModal("verification-modal");
}

function updateProfilePic() {
    $("#profilePictureUpload").click();
}

function resizeAudio(file, callback) {
	if (file.type == "audio/mpeg") {
		try {
			let cutter = new mp3cutter();
			cutter.cut(file, 0, maxAudioSeconds, function(cut) {
				getBase64(cut, callback);
			});
		} catch (e) {
			console.log(e);
			getBase64(file, callback);
		}
	} else if (file.type == "audio/x-wav" || file.type == "audio/wav") {
		getBase64(file, callback);
	} else {
		hideLoader();
		alert(getText("error.format-not-supported"));
	}
}

function resizeImage(file, callback) {

	if (window.HTMLCanvasElement && window.CanvasRenderingContext2D) {
		var reader = new FileReader();
		reader.onload = function(readerEvent) {
			var img = new Image();
			img.onload = function() {
				let canvas = document.createElement('canvas');
				let width = img.width;
				let height = img.height;
				let sx = 0;
				let sy = 0;

				if (width > height) {
					sx = width / 2 - height / 2;
					width = height;
				} else {
					sy = height / 2 - width / 2;
					height = width;
				}

				canvas.height = maxImageSize;
				canvas.width = maxImageSize;

				canvas.getContext('2d').drawImage(img,
					sx, sy, width, height,
					0, 0, maxImageSize, maxImageSize);

				if (canvasProtected(canvas.getContext('2d'))) {
					getBase64(file, callback);
				} else {
					let dataUrl = canvas.toDataURL('image/jpeg');
					callback(dataUrl);
				}
			}
			img.src = readerEvent.target.result;
		}
		reader.readAsDataURL(file);
	} else {
		getBase64(file, callback);
	}
}

function getBase64InMB(base64) {
	return (base64.length * (3 / 4) - 1) / 1000000;
}

function getBase64(file, callback) {
	var reader = new FileReader();
	reader.readAsDataURL(file);
	reader.onload = function() {
		callback(reader.result);
	};
	reader.onerror = function() {
	};
}

function canvasProtected(context) {
	let data = context.getImageData(1, 1, 1, 1).data;
	let data2 = context.getImageData(1, 1, 1, 1).data;
	if (data[0] == data2[0] && data[1] == data2[1] && data[2] == data2[2]) {
		return false;
	} else {
		return true;
	}
}
