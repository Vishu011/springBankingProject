package com.omnibank.onboarding.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * Body for uploading documents. The applicationId comes from the URL path.
 */
@Data
public class DocumentUploadRequest {

  @NotEmpty
  @Size(max = 10)
  private List<@Valid DocumentItem> documents;

  @Data
  public static class DocumentItem {
    @Size(max = 40)
    private String type; // PASSPORT, DRIVER_LICENSE, etc.

    @Size(max = 2048)
    private String url;
  }
}
