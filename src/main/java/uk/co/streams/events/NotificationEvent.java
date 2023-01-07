package uk.co.streams.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationEvent {
  private String sku;
  private long value;
}
