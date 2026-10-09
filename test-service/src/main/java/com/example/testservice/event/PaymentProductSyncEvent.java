package com.example.testservice.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProductSyncEvent(UUID seriesId, String title, boolean active, BigDecimal basePriceRupees) {}
