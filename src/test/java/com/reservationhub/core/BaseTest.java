package com.reservationhub.core;

import com.reservationhub.controllers.apicontroller.AuthApiController;
import com.reservationhub.controllers.apicontroller.BookingApiController;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.core.utils.ApiReadyCheck;
import com.reservationhub.core.utils.BookingCleanup;
import common.extent.ExtentITestListenerAdapter;
import common.listeners.AllureGroupTags;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import java.lang.reflect.Method;

/**
 * Base class of every test module. Every test creates the data it needs, and {@link #tearDown()} removes it
 * afterwards, so no test relies on the sandbox's seed data or on another test having run first.
 */
@Listeners({ExtentITestListenerAdapter.class, AllureGroupTags.class})
public abstract class BaseTest {

    protected final BookingApiController bookingController = new BookingApiController();
    protected final AuthApiController authController = new AuthApiController();

    /** Runs once before the whole suite: waits out the sandbox's cold start, or skips every test if it never wakes. */
    @BeforeSuite(alwaysRun = true)
    public void waitForApi() {
        ApiReadyCheck.ensureApiIsUp();
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method m) {
        System.out.println("STARTING TEST: " + getClass().getSimpleName() + "." + m.getName());
    }

    /** alwaysRun: clean up after failed tests too, and when running a group subset with -Dgroups. */
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        BookingCleanup.deleteAll();
        BookingTokenManager.invalidateToken();
    }
}
