// com.aiaca.btop.stt.MockSttService
package com.aiaca.btop.stt;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.concurrent.*;

@Service
@Profile("mock") // <- mock 프로필일 때만 등록
public class MockSttService implements SttService {
  private Listener listener;
  private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
  private volatile boolean closed = false;

  @Override public void start(Listener listener) {
    this.listener = listener;
    ses.scheduleAtFixedRate(() -> {
      if (closed || this.listener == null) return;
      String raw = "테스트 음성 인식 중...";
      String std = "테스트 음성 인식 중입니다.";
      this.listener.onPartial(raw, std);
    }, 500, 700, TimeUnit.MILLISECONDS);
  }

  @Override public void feedPcm(byte[] data, int len) { /* no-op */ }

  @Override public void stopAndFinalize() {
    if (listener != null) listener.onFinal("안녕하셨지라(TEST)", "안녕하세요(TEST)");
  }

  @Override public void close() throws IOException {
    closed = true;
    ses.shutdownNow();
  }
}
