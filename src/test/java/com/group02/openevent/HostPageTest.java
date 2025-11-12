package com.group02.openevent;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HostPageTest {
    private WebDriver driver;

    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
    }

    @AfterEach
    void cleanup() {
        driver.quit();
    }

    @Test
    public void testGotoEventAfterLogin() {

        driver.get("http://localhost:8080/login");

        driver.findElement(By.name("username")).sendKeys("john.doe@example.com");

        driver.findElement(By.name("password")).sendKeys("123456");

        driver.findElement(By.id("loginBtn")).click();

        driver.findElement(By.id("createEventBtn")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement header = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1.page-title"))
        );

        Assertions.assertEquals("Sự kiện", header.getText().trim());
    }

    @Test
    void testOpenEventsPage() {
        testGotoEventAfterLogin();

        WebElement header =
                driver.findElement(By.cssSelector("h1.page-title"));

        Assertions.assertEquals("Sự kiện", header.getText().trim());
    }

    @Test
    void testOpenCreateEventModal() {

        testGotoEventAfterLogin();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Đảm bảo page loaded
        wait.until(ExpectedConditions
                .textToBePresentInElementLocated(
                        By.cssSelector("h1.page-title"),
                        "Sự kiện"
                ));

        // Click button
        WebElement btn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector(".btn-create-event")
                )
        );
        btn.click();

        // JS fallback
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelector('.btn-create-event').click();");
        } catch (Exception ignored) {
        }

        // Wait modal present
        WebElement modal = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("createEventModal")
                )
        );

        wait.until(ExpectedConditions.visibilityOf(modal));

        Assertions.assertTrue(modal.isDisplayed());
    }

    @Test
    void testCreateEventForm() {

        testGotoEventAfterLogin();     // đang ở trang Sự kiện


        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Nhập vào form
        WebElement title = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.name("title"))
        );
        title.sendKeys("New Event Selenium");

        // Select eventType
        Select type = new Select(driver.findElement(By.name("eventType")));
        type.selectByVisibleText("MUSIC");

        driver.findElement(By.name("startsAt")).sendKeys("2030-11-11T10:00");
        driver.findElement(By.name("endsAt")).sendKeys("2030-11-11T12:00");
        driver.findElement(By.name("enrollDeadline")).sendKeys("2030-11-09T12:00");

        driver.findElement(By.cssSelector(".submit-btn")).click();

        // Chờ redirect và hiển thị event mới
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector(".events-list"),
                "New Event Selenium"
        ));
    }


}
