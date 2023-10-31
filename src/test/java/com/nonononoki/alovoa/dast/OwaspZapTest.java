package com.nonononoki.alovoa.dast;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.zaproxy.clientapi.core.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource("classpath:application-dev.properties")
@Transactional
public class OwaspZapTest {
    private static final Logger logger = LoggerFactory.getLogger(OwaspZapTest.class);

    private static final String ZAP_ADDRESS = "localhost";
    private static final String ZAP_API_KEY = "NZax2DPZt41UmLTeHi4nqYUSIPGdFIGw";
    private static ClientApi api;

    public static final DockerImageName OWASPZAP_IMAGE = DockerImageName.parse("softwaresecurityproject/zap-stable:latest");

    private static final int OWASP_PORT = 8080;

    private static final List<String> zapEnv = new ArrayList<>();

    @Value("${app.admin.email}")
    private String EMAIL;

    @Value("${app.admin.key}")
    private String PASSWORD;

    @Value("${app.domain}")
    private String appDomain;


    // Beware of Zap is running under Docker. Real report path is ./target/test-classes/zap-report.html
    private static final String DOCKER_REPORT_PATH = "/tmp/reports";

    private static ChromeDriver chromeDriver;

    @LocalServerPort
    private int alovoaPort;


    private static void executeSpiderScan(String targetUrl) {
        System.out.println("Spidering target : " + targetUrl);
        String scanId;

        try {
            int progress;
            ApiResponse response = api.spider.scan(targetUrl, null, null, null, null);
            scanId = ((ApiResponseElement) response).getValue();
            do {
                Thread.sleep(1000);
                progress = Integer.parseInt(((ApiResponseElement) api.spider.status(scanId)).getValue());
                logger.info(String.format("Spider progress : %s %%", progress));
            } while (progress < 100);
            logger.info("Spider completed!");
            List<ApiResponse> spiderResults = ((ApiResponseList) api.spider.results(scanId)).getItems();
            logger.info("Following resources have been found:");
            spiderResults.forEach(System.out::println);
        } catch (Exception e) {
            logger.error(String.format("%s", e.getMessage()));
        }
    }

    public static void executeActiveScan(String targetUrl) {
        logger.info(String.format("Active scanning target : %s", targetUrl));
        String scanId;
        try {
            int progress;
            ApiResponse response = api.ascan.scan(targetUrl,
                    "True",
                    "False",
                    null,
                    null,
                    null
            );
            scanId = ((ApiResponseElement) response).getValue();
            do {
                Thread.sleep(5000);
                progress = Integer.parseInt(((ApiResponseElement) api.ascan.status(scanId)).getValue());
                logger.info(String.format("Active scan progress : %s %%", progress));
            } while (progress < 100);
            logger.info("Active scan completed!");
        } catch (ClientApiException | InterruptedException e) {
            logger.error(String.format("Exception caught: %s", e.getMessage()));
        }
    }

    @BeforeAll
    public static void setup() throws ClientApiException {
        zapEnv.add(String.format("ZAP_PORT=%s", OWASP_PORT));
        GenericContainer<?> container = new GenericContainer<>(OWASPZAP_IMAGE);
        container.setEnv(zapEnv);
        container.withExposedPorts(OWASP_PORT);
        container.withLogConsumer(new Slf4jLogConsumer(logger));
        container.withClasspathResourceMapping(".", DOCKER_REPORT_PATH, BindMode.READ_WRITE);
        container.setCommand(String.format("zap.sh -daemon -host 0.0.0.0 -port 8080 -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true -config api.key=%s", ZAP_API_KEY));
        container.start();

        int zapPort = container.getMappedPort(OWASP_PORT);
        api = new ClientApi(ZAP_ADDRESS, zapPort, ZAP_API_KEY);

        // We do not want to scan Google, Facebook etc.
        api.core.excludeFromProxy("^.*\\.googleapis\\.com.*$");
        api.core.excludeFromProxy("^.*\\.google\\.com.*$");
        api.core.excludeFromProxy("^.*\\.facebook\\.com.*$");
        api.core.excludeFromProxy("^.*\\.fbcdn\\.net.*$");
        api.core.excludeFromProxy("^.*\\.ip6\\.li.*$");

        ChromeOptions chromeOptions = getChromeOptions(zapPort);
        ChromeDriverService chromeDriverService = getChromeDriverService();
        chromeDriver = new ChromeDriver(chromeDriverService, chromeOptions);
    }

    // This is the glue between Selenium and OWASP Zap
    @NotNull
    private static ChromeOptions getChromeOptions(int zapPort) {
        String proxyServerUrl = String.format("%s:%d", OwaspZapTest.ZAP_ADDRESS, zapPort);
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(proxyServerUrl);
        proxy.setSslProxy(proxyServerUrl);

        ChromeOptions chromeOptions = new ChromeOptions();
        // OWASP Zap proxy uses a self signed server certificate
        chromeOptions.addArguments("--ignore-ssl-errors=yes");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--lang=en-US,en");
        chromeOptions.setAcceptInsecureCerts(true);
        chromeOptions.setProxy(proxy);
        return chromeOptions;
    }

    @NotNull
    private static ChromeDriverService getChromeDriverService() {
        return new ChromeDriverService.Builder()
                .withLogOutput(System.err)
                .build();
    }

    @AfterAll
    public static void closeAll() throws InterruptedException {
        String title = "ZAP Selenium";
        String template = "traditional-html";
        String description = "This is a ZAP report for Alovoa";
        String reportFilename = "zap-report.html";
        try {
            ApiResponse res = api.reports.generate(
                    title,
                    template,
                    null,
                    description,
                    null,
                    null,
                    null,
                    null,
                    null,
                    reportFilename,
                    null,
                    DOCKER_REPORT_PATH, null);
            logger.info(String.format("ZAP report generated here: %s", res.toString()));
        } catch (ClientApiException ex) {
            logger.error(String.format("closeAll report: %s", ex.getMessage()));
        }

        Thread.sleep(2000);

        chromeDriver.quit();
    }

    @Test
    public void alovoaSecurityAssessment() throws InterruptedException {

        // localhost does not work here, because OWASP Zap is running in a container
        final String targetUrl = String.format("%s:%d", appDomain, alovoaPort);

        ChromeDriver driver = chromeDriver;
        driver.get(targetUrl);
        OwaspZapTest.executeSpiderScan(targetUrl);
        OwaspZapTest.executeActiveScan(targetUrl);
        Thread.sleep(2000);
        driver.findElement(By.id("login")).click();
        Thread.sleep(2000);
        driver.findElement(By.name("username")).sendKeys(EMAIL);
        driver.findElement(By.name("password")).sendKeys(PASSWORD);
        driver.findElement(By.id("send_login")).click();

        Thread.sleep(2000);
        if (driver.findElement(By.linkText("Search")).isDisplayed()) {
            OwaspZapTest.executeSpiderScan(targetUrl);
            OwaspZapTest.executeActiveScan(targetUrl);
        }
    }

}
