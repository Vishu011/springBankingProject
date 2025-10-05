package com.omnibank.productcatalog.api;

import com.omnibank.productcatalog.application.ProductService;
import com.omnibank.productcatalog.application.ProductService.View;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investments/products")
@RequiredArgsConstructor
public class ProductController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final ProductService service;

  @GetMapping
  public ResponseEntity<List<View>> list(@RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId) {
    String cid = ensureCorrelationId(correlationId);
    return ResponseEntity.ok().header(HDR_CORRELATION_ID, cid).body(service.list());
  }

  @GetMapping("/{code}")
  public ResponseEntity<View> get(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("code") String code
  ) {
    String cid = ensureCorrelationId(correlationId);
    return ResponseEntity.ok().header(HDR_CORRELATION_ID, cid).body(service.get(code));
  }

  // Dev-open: refresh mock NAVs
  @PostMapping("/internal/dev/refresh-navs")
  public ResponseEntity<List<View>> refreshNavs(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId
  ) {
    String cid = ensureCorrelationId(correlationId);
    return ResponseEntity.ok().header(HDR_CORRELATION_ID, cid).body(service.refreshNavs());
  }

  private static String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }
}
