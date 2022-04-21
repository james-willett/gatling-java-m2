package gatlingdemostore.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class Customer {

    private static final FeederBuilder<String> csvFeederLoginDetails =
            csv("data/loginDetails.csv").circular();
    public static final ChainBuilder login =
            feed(csvFeederLoginDetails)
                    .exec(http("Load Login Page")
                            .get("/login")
                            .check(substring("Username:")))
                    .exec(
                            http("Customer Login Action")
                                    .post("/login")
                                    .formParam("_csrf", "#{csrfValue}")
                                    .formParam("username", "#{username}")
                                    .formParam("password", "#{password}"))
                    .exec(session -> session.set("customerLoggedIn", true));
}
