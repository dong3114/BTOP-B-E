// JJ
package com.aiaca.btop.stt;

import java.io.Closeable;

public interface SttService extends Closeable {
  interface Listener {
    void onPartial(String text);
    void onFinal(String text);
    void onError(String message, Throwable t);
  }
  void start(Listener listener);            // 세션 시작
  void feedPcm(byte[] pcm, int len);        // 16kHz mono s16le
  void stopAndFinalize();                   // 세션 종료 & 최종 변환
}
