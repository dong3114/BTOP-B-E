//JJ
package com.aiaca.btop.stt;

import java.util.concurrent.*;

public class MockSttService implements SttService {
  private Listener listener;
  private final ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
  private long bytes;
  private ScheduledFuture<?> ticker;

  @Override public void start(Listener l) {
    this.listener = l;
    // 1초마다 partial 토막 전송
    ticker = es.scheduleAtFixedRate(() -> {
      if (listener != null) listener.onPartial("bytes=" + bytes);
    }, 1, 1, TimeUnit.SECONDS);
  }

  @Override public void feedPcm(byte[] pcm, int len) {
    bytes += len;
  }

  @Override public void stopAndFinalize() {
    if (listener != null) listener.onFinal("final bytes=" + bytes);
  }

  @Override public void close() {
    if (ticker != null) ticker.cancel(true);
    es.shutdownNow();
  }
}
