package uk.co.streams.events;

import lombok.Data;

@Data
public class SaleEvent {
  private String product;
  private Long value;
}
