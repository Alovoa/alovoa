const twa_playstore = "android-app://com.alovoa.alovoa_playstore";

$(function() {

	bulmaCollapsible.attach();

	let cookie = localStorage.getItem('cookie');
	if (!cookie) {
		$('#cookie-banner').show();
	}

	let url = window.location.href;
	let param;

	if (url.includes("?confirm-registration")) {
		param = "index.js.confirm-registration";
	} else if (url.includes("?registration-confirm-success")) {
		param = "index.js.registration-confirm-success";
	} else if (url.includes("?registration-confirm-failed")) {
		param = "index.js.registration-confirm-failed";
	} else if (url.includes("?password-reset-requested")) {
		param = "index.js.password-reset-requested";
	} else if (url.includes("?password-change-success")) {
		param = "index.js.password-change-success";
	} else if (url.includes("?confirm-account-deleted")) {
		param = "index.js.confirm-account-deleted";
	}

	if (param) {
		let text = getText(param);
		if (text) {
			alert(text);
		}
	}

	if ('serviceWorker' in navigator) {
		navigator.serviceWorker.register('/sw.js');
	};

	if (document.referrer.includes(twa_playstore)) {
		docCookies.setItem("twa_playstore", "true");
	}

	showIosPwaBanner();
});

function cookieClick() {
	localStorage.setItem('cookie', true);
	$('#cookie-banner').css("visibility", "hidden");
}

function showIosPwaBanner() {
	let userAgent = window.navigator.userAgent;
	let isIos = /iPhone|iPad|iPod/.test(userAgent);
	console.log("isIos:" + isIos);
	if (isIos) {
		let isPwa = window.location.href.includes("?pwa");
		if (isPwa) {
			localStorage.setItem("pwa", true);
		} else {
			isPwa = localStorage.getItem("pwa");
		}
		console.log("isPwa:" + isPwa);
		if (!isPwa) {
			openModal("ios-pwa-modal");
		}
	}
}