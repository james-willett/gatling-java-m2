package gatlingdemostore.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.CoreDsl.substring;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class Catalog {

    private static final FeederBuilder<String> categoryFeeder =
            csv("data/categoryDetails.csv").random();

    private static final FeederBuilder<Object> jsonFeederProducts =
            jsonFile("data/productDetails.json").random();

    public static class Category {
        public static final ChainBuilder view =
                feed(categoryFeeder)
                        .exec(
                                http("Load Category Page - #{categoryName}")
                                        .get("/category/#{categorySlug}")
                                        .check(css("#CategoryName").isEL("#{categoryName}")));
    }

    public static class Product {
        public static final ChainBuilder view =
                feed(jsonFeederProducts)
                        .exec(
                                http("Load Product Page - #{name}")
                                        .get("/product/#{slug}")
                                        .check(css("#ProductDescription").isEL("#{description}")));

        public static final ChainBuilder add =
                exec(view)
                        .exec(
                                http("Add Product to Cart")
                                        .get("/cart/add/#{id}")
                                        .check(substring("items in your cart")))
                        .exec(
                                session -> {
                                    double currentCartTotal = session.getDouble("cartTotal");
                                    double itemPrice = session.getDouble("price");
                                    return session.set("cartTotal", (currentCartTotal + itemPrice));
                                });
    }
}
