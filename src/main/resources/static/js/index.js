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
		localStorage.setItem("twa_playstore", true);
	}

	showIosPwaBanner();
	hero();
});

function cookieClick() {
	localStorage.setItem('cookie', true);
	$('#cookie-banner').css("visibility", "hidden");
}

function showIosPwaBanner() {
	let userAgent = window.navigator.userAgent;
	let isIos = /iPhone|iPad|iPod/.test(userAgent);
	if (isIos) {
		let isPwa = window.location.href.includes("?pwa");
		if (isPwa) {
			localStorage.setItem("pwa", true);
		} else {
			isPwa = localStorage.getItem("pwa");
		}
		if (!isPwa) {
			openModal("ios-pwa-modal");
		}
	}
}

var textures = [];//generate strings when the script is first run
for (var i = 0; i < 10; i++) textures.push('./img/profile/' + (i + 1) + '.png');
//Based on https://github.com/liabru/matter-js/blob/master/examples/sprites.js , MIT license
function hero() {

	let width = window.innerWidth;
	let height = window.innerHeight - 56;
	let isMobile = width <= 500 || height <= 500;
	let multiplicator = 1;
	if (isMobile) {
		multiplicator = 0.6;
	}

	var Engine = Matter.Engine,
		Render = Matter.Render,
		Runner = Matter.Runner,
		Composites = Matter.Composites,
		Composite = Matter.Composite,
		Events = Matter.Events,
		Body = Matter.Body,
		Bodies = Matter.Bodies;

	// create engine
	var engine = Engine.create(),
		world = engine.world;
	engine.gravity.y = -1.0;
	engine.enableSleeping = true;

	// create renderer
	var render = Render.create({
		element: document.getElementById("hero-gravity"),
		engine: engine,
		options: {
			width: width,
			height: height,
			showAngleIndicator: false,
			showSleeping: false,
			wireframes: false
		}
	});

	Render.run(render);

	// create runner
	var runner = Runner.create();
	Runner.run(runner, engine);

	// add bodies
	let options = {
		isStatic: true,
		render: {
			visible: true
		}
	};

	world.bodies = [];

	Composite.add(world, [
		Bodies.rectangle(width, 0, width * 2, 1, options),
		Bodies.rectangle(width + 1, 0, 1, height * 2, options),
		Bodies.rectangle(0 - 1, 0, 1, height * 2, options)
	]);

	var stack = Composites.stack(10, height, //xx,yy
		width / 100 / multiplicator, height / 150 / multiplicator, //columns, rows
		50, 200, //colGap, rowGap
		function(x, y) {
			let rand = Math.floor(Math.random() * 10); //generates number from 0 to 9
			return Bodies.circle(x, y, 80 * multiplicator, {
				render: {
					strokeStyle: '#ffffff',
					sprite: {
						texture: textures[rand],
						xScale: 0.5 * multiplicator,
						yScale: 0.5 * multiplicator
					}
				}
			});
		});

	Composite.add(world, stack);
	
	Events.on(engine, 'collisionStart', (event) => {
		event.pairs.forEach((collision) => {
			setTimeout(function() {
			  	Body.setStatic(collision.bodyA, true);
				Body.setStatic(collision.bodyB, true);
			}, 5000);
			
		});
	});

}
