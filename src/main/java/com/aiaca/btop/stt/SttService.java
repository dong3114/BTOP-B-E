package com.aiaca.btop.stt;

import java.io.Closeable;
import java.io.IOException;

public interface SttService extends Closeable {

  interface Listener {
    /** 부분 결과(원문/표준어). 표준어가 아직 없으면 standardText는 null 또는 빈 문자열로 줄 수 있음 */
    void onPartial(String rawText, String standardText);

    /** 최종 결과(원문/표준어) */
    void onFinal(String rawText, String standardText);

    /** 오류 */
    void onError(String msg, Throwable t);
  }

  /** 세션 시작 (콜백 등록) */
  void start(Listener listener) throws IOException;

  /** 16kHz mono s16le PCM 바이트 공급 */
  void feedPcm(byte[] data, int len);

  /** 세션 최종화(엔진에 flush/commit 유도) */
  void stopAndFinalize();

  /** 리소스 정리 */
  @Override
  void close() throws IOException;
}
