package com.omnibank.productcatalog.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

  private final ConcurrentHashMap<String, Product> store = new ConcurrentHashMap<>();

  public ProductService() {
    // Seed a few products for dev-open
    upsert(new Product("FND-1001", "Omni Balanced Fund", "MUTUAL_FUND", new BigDecimal("100.00"), Instant.now()));
    upsert(new Product("ETF-500", "Omni Nifty 50 ETF", "ETF", new BigDecimal("200.00"), Instant.now()));
    upsert(new Product("BND-10Y", "Omni 10Y Bond", "BOND", new BigDecimal("1000.00"), Instant.now()));
  }

  public List<View> list() {
    return store.values().stream().map(ProductService::toView).toList();
  }

  public View get(String productCode) {
    Product p = store.get(productCode);
    if (p == null) throw new IllegalArgumentException("Product not found: " + productCode);
    return toView(p);
  }

  // Dev-only: mock NAV update (+/- 1% random)
  public List<View> refreshNavs() {
    List<View> result = new ArrayList<>();
    for (Product p : store.values()) {
      BigDecimal nav = p.nav();
      BigDecimal delta = nav.multiply(new BigDecimal(Math.random() * 0.02 - 0.01));
      BigDecimal updated = nav.add(delta).max(new BigDecimal("0.01"));
      Product upd = new Product(p.code(), p.name(), p.type(), updated, Instant.now());
      upsert(upd);
      result.add(toView(upd));
    }
    return result;
  }

  private void upsert(Product p) {
    store.put(p.code(), p);
  }

  private static View toView(Product p) {
    return new View(p.code(), p.name(), p.type(), p.nav(), p.navTs());
  }

  public record Product(
      String code,          // e.g., FND-1001, ETF-500
      String name,          // display name
      String type,          // MUTUAL_FUND | ETF | BOND
      BigDecimal nav,       // simplified value / NAV
      Instant navTs         // timestamp of last update
  ) {}

  public record View(
      String code,
      String name,
      String type,
      BigDecimal nav,
      Instant navTs
  ) {}
}
