package com.example.app.billing;

import java.time.LocalDate;

public class BillingPolicy {
  public int refundWindowDays = 14;

  public boolean eligibleForRefund(LocalDate purchaseDate, LocalDate today) {
    return purchaseDate != null && !purchaseDate.isAfter(today) && purchaseDate.plusDays(refundWindowDays).isAfter(today.minusDays(1));
  }
}
