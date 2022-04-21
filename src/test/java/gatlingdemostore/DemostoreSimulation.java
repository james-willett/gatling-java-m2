package gatlingdemostore;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.gatling.javaapi.jdbc.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static io.gatling.javaapi.jdbc.JdbcDsl.*;

import gatlingdemostore.pageobjects.Customer;
import gatlingdemostore.pageobjects.Catalog;
import gatlingdemostore.pageobjects.CmsPages;
import gatlingdemostore.pageobjects.Checkout;

public class DemostoreSimulation extends Simulation {

  private static final String DOMAIN = "demostore.gatling.io";
  private static final HttpProtocolBuilder HTTP_PROTOCOL = http.baseUrl("https://" + DOMAIN);

  private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "3"));

  private static final Duration RAMP_DURATION =
          Duration.ofSeconds(Integer.parseInt(System.getProperty("RAMP_DURATION", "10")));

  private static final Duration TEST_DURATION =
          Duration.ofSeconds(Integer.parseInt(System.getProperty("DURATION", "60")));

  @Override
  public void before() {
    System.out.printf("Running test with %d users%n", USER_COUNT);
    System.out.printf("Ramping users over %d seconds%n", RAMP_DURATION.getSeconds());
    System.out.printf("Total test duration: %d seconds%n", TEST_DURATION.getSeconds());
  }

  @Override
  public void after() {
    System.out.println("Stress testing complete");
  }

  private static final ChainBuilder initSession =
          exec(flushCookieJar())
                  .exec(session -> session.set("randomNumber", ThreadLocalRandom.current().nextInt()))
                  .exec(session -> session.set("customerLoggedIn", false))
                  .exec(session -> session.set("cartTotal", 0.00))
                  .exec(addCookie(Cookie("sessionId", SessionId.random()).withDomain(DOMAIN)));

  private static final ScenarioBuilder scn = scenario("DemostoreSimulation")
    .exec(initSession)
    .exec(CmsPages.homepage)
    .pause(2)
    .exec(CmsPages.aboutUs)
    .pause(2)
    .exec(Catalog.Category.view)
    .pause(2)
    .exec(Catalog.Product.add)
    .pause(2)
    .exec(Checkout.viewCart)
    .pause(2)
    .exec(Checkout.completeCheckout);

  private static class UserJourneys {

    private static final Duration MIN_PAUSE = Duration.ofMillis(100);
    private static final Duration MAX_PAUSE = Duration.ofMillis(500);

    private static final ChainBuilder browseStore =
            exec(initSession)
                    .exec(CmsPages.homepage)
                    .pause(MAX_PAUSE)
                    .exec(CmsPages.aboutUs)
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .repeat(5)
                      .on(
                            exec(Catalog.Category.view)
                            .pause(MIN_PAUSE, MAX_PAUSE)
                            .exec(Catalog.Product.view)
                    );

    private static final ChainBuilder abandonCart =
            exec(initSession)
                    .exec(CmsPages.homepage)
                    .pause(MAX_PAUSE)
                    .exec(Catalog.Category.view)
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Catalog.Product.view)
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Catalog.Product.add);

    private static final ChainBuilder completePurchase =
            exec(initSession)
                    .exec(CmsPages.homepage)
                    .pause(MAX_PAUSE)
                    .exec(Catalog.Category.view)
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Catalog.Product.view)
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Catalog.Product.add)
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Checkout.viewCart)
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Checkout.completeCheckout);
  }

  private static class Scenarios {
    private static final ScenarioBuilder defaultPurchase =
            scenario("Default Load Test")
                    .during(TEST_DURATION)
                    .on(
                            randomSwitch()
                                    .on(
                                            Choice.withWeight(75.0, exec(UserJourneys.browseStore)),
                                            Choice.withWeight(15.0, exec(UserJourneys.abandonCart)),
                                            Choice.withWeight(10.0, exec(UserJourneys.completePurchase))));

    private static final ScenarioBuilder highPurchase =
            scenario("High Purchase Load Test")
                    .during(Duration.ofSeconds(60))
                    .on(
                            randomSwitch()
                                    .on(
                                            Choice.withWeight(25.0, exec(UserJourneys.browseStore)),
                                            Choice.withWeight(25.0, exec(UserJourneys.abandonCart)),
                                            Choice.withWeight(50.0, exec(UserJourneys.completePurchase))));
  }

  {
    setUp(
            Scenarios.defaultPurchase.injectOpen(rampUsers(USER_COUNT).during(RAMP_DURATION)),
            Scenarios.highPurchase.injectOpen(rampUsers(2).during(Duration.ofSeconds(10))))
            .protocols(HTTP_PROTOCOL);
  }
}
