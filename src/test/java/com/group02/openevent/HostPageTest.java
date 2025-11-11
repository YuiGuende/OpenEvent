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

        testGotoEventAfterLogin(); // đã điều hướng tới trang Sự kiện

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // 1) Đảm bảo đúng trang
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("h1.page-title"), "Sự kiện"));

        // 2) Chờ nút hiện diện rồi mới clickable
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".btn-create-event")));
        WebElement createBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(".btn-create-event")));

        // 3) Click thường + fallback JS
        try {
            createBtn.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", createBtn);
        }

        // 4) Chờ modal hiện
        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("createEventModal")));

        Assertions.assertTrue(modal.isDisplayed());
    }



    @Test
    void testCreateEventForm() {

        testGotoEventAfterLogin();  // đang ở trang Sự kiện
        testOpenCreateEventModal(); // modal mở

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement title = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.name("title"))
        );
        title.sendKeys("New Event Selenium");

        // eventType
        Select type = new Select(driver.findElement(By.name("eventType")));
        type.selectByVisibleText("MUSIC");

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                "arguments[0].value='2030-11-11T10:00';",
                driver.findElement(By.name("startsAt"))
        );
        js.executeScript(
                "arguments[0].value='2030-11-11T12:00';",
                driver.findElement(By.name("endsAt"))
        );
        js.executeScript(
                "arguments[0].value='2030-11-09T12:00';",
                driver.findElement(By.name("enrollDeadline"))
        );

        driver.findElement(By.cssSelector(".submit-btn")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector(".events-list"),
                "New Event Selenium"
        ));
    }



}
