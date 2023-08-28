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

import java.util.ArrayList;
import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@Transactional
public class OwaspZapTest {
    private static final Logger logger = LoggerFactory.getLogger(OwaspZapTest.class);

    private static final String ZAP_ADDRESS = "localhost";
    private static final String ZAP_API_KEY = "NZax2DPZt41UmLTeHi4nqYUSIPGdFIGw";
    private static ClientApi api;

    public static final DockerImageName OWASPZAP_IMAGE = DockerImageName.parse("softwaresecurityproject/zap-stable");

    private static final int OWASP_PORT = 8080;

    private static final List<String> zapEnv = new ArrayList<>();

    @Value("${app.admin.email}")
    private String EMAIL;

    @Value("${app.admin.key}")
    private String PASSWORD;

    // Beware of Zap is running under Docker. Real report path is ./target/test-classes/zap-report.html
    private static final String REPORT_PATH = "/tmp/reports";

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
        } catch (Exception e) {
            logger.error(String.format("Exception caught: %s", e.getMessage()));
        }
    }

    @BeforeAll
    public static void setup() {
        zapEnv.add(String.format("ZAP_PORT=%s", OWASP_PORT));
        GenericContainer<?> container = new GenericContainer<>(OWASPZAP_IMAGE);
        container.setEnv(zapEnv);
        container.withExposedPorts(OWASP_PORT);
        container.withLogConsumer(new Slf4jLogConsumer(logger));
        container.withClasspathResourceMapping(".", REPORT_PATH, BindMode.READ_WRITE);
        container.setCommand(String.format("zap.sh -daemon -host 0.0.0.0 -port 8080 -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true -config api.key=%s", ZAP_API_KEY));
        container.start();

        int zapPort = container.getMappedPort(OWASP_PORT);
        api = new ClientApi(ZAP_ADDRESS, zapPort, ZAP_API_KEY);
        ChromeOptions chromeOptions = getChromeOptions(zapPort);

        ChromeDriverService chromeDriverService = new ChromeDriverService.Builder()
                .withLogOutput(System.out)
                .build();

        chromeDriver = new ChromeDriver(chromeDriverService, chromeOptions);

    }

    @NotNull
    private static ChromeOptions getChromeOptions(int zapPort) {
        String proxyServerUrl = OwaspZapTest.ZAP_ADDRESS + ":" + zapPort;
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(proxyServerUrl);
        proxy.setSslProxy(proxyServerUrl);

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--ignore-ssl-errors=yes");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.setAcceptInsecureCerts(true);
        chromeOptions.setProxy(proxy);
        return chromeOptions;
    }

    @AfterAll
    public static void closeAll() throws InterruptedException {
        String title = "ZAP Selenium";
        String template = "traditional-html";
        String description = "This is a ZAP report for Alovoa";
        String reportfilename = "zap-report.html";
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
                    reportfilename,
                    null,
                    REPORT_PATH, null);
            System.out.println("ZAP report generated here: " + res.toString());
        } catch (ClientApiException ex) {
            logger.error(String.format("closeAll report: %s", ex.getMessage()));
        }

        Thread.sleep(2000);

        chromeDriver.quit();
    }

    @Test
    public void alovoaSecurityAssessment() throws InterruptedException {

        final String targetUrl = String.format("http://alovoa.test.felsing.net:%d", alovoaPort);

        ChromeDriver driver = chromeDriver;
        driver.get(targetUrl);
        OwaspZapTest.executeSpiderScan(targetUrl);
        OwaspZapTest.executeActiveScan(targetUrl);
        Thread.sleep(2000);
        driver.findElement(By.linkText("Anmelden")).click();
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
