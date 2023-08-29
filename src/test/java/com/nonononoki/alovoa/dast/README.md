# Dynamic Application Security Test (DAST)

Today it is essential to do security tests for every application.
This directory contains a solution for dynamic application security tests (DAST) with
[OWASP Zap](https://www.zaproxy.org/) (Zed Attack Proxy) provides by OWASP. This
unit test uses following components:

* [TestContainers](https://java.testcontainers.org/)
* [Selenium](https://www.selenium.dev/)
* [Dockerized OWASP Zap](https://hub.docker.com/r/softwaresecurityproject/zap-stable)

There is a tight interaction between Selenium an OWASP Zap, which needs some explanation to understand
what happens here.

* Selenium is an automation platform which allows to control a web browser by Java code, in this case a unit test.
* Selenium configures browser to use a proxy server. Proxy server is OWASP Zap, which is configured to attack Alovoa on localhost.
* Now it should be obvious that Alovoa must be fully configured and operable, so it can be tested.

# False Positives

OWASP Zap delivers a report which shows some security flaws. That means from a network view on application.
There may be possible security flaws. Every finding must be checked carefully.

# Alovoa HTML

To do effective tests, Selenium needs a little bit help. That means all input fields regarding authentication needs
an id so Selenium is able to enter authn data. Other input fields are attacked by OWASP Zap automatically.

# Running Test

While running this unit test a Chrome or Chromium browser will appear on screen. **Do not try to interact with
that browser window**. Otherwise you will unexpected results.

While tests application may throw many exceptions like

    ERROR c.n.a.component.ExceptionHandler - java.lang.IllegalArgumentException: Locale part "Set-cookie:" contains invalid characters

This is ok, in a real attack such messages will appear in your log file and you may trigger a fail2ban rule.
