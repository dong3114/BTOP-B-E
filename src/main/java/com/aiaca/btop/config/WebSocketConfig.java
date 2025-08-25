//JJ
package com.aiaca.btop.config;

import com.aiaca.btop.ws.EchoWsHandler;
import com.aiaca.btop.ws.SttWsHandler;
import com.aiaca.btop.stt.SttService;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Bean
  public EchoWsHandler echoWsHandler() {
    return new EchoWsHandler();
  }


  // ---- 주입받아 쓸 필드들 ----
  @Autowired
  private ObjectFactory<SttService> sttFactory; // 연결마다 새로운 STT 인스턴스 생성용

  @Value("${stt.ffmpeg.path:ffmpeg}")
  private String ffmpegPath;                    // ffmpeg 경로 주입 (없으면 PATH의 ffmpeg 사용)

  // ---- 무인자 Bean 메서드: registerWebSocketHandlers에서 바로 호출 가능 ----
  @Bean
  public SttWsHandler sttWsHandler() {
    return new SttWsHandler(sttFactory, ffmpegPath);
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(echoWsHandler(), "/ws/echo")
            .setAllowedOriginPatterns("*");
    registry.addHandler(sttWsHandler(), "/ws/stt")
            .setAllowedOriginPatterns("*");
  }

  @Bean
public ServletServerContainerFactoryBean wsContainer() {
    ServletServerContainerFactoryBean c = new ServletServerContainerFactoryBean();
    // 1MB까지 한 메시지 허용 (필요하면 더 키워도 됨)
    c.setMaxBinaryMessageBufferSize(1024 * 1024);
    // 텍스트 버퍼도 여유있게
    c.setMaxTextMessageBufferSize(64 * 1024);
    // send 타임아웃(비동기 전송 시간 한도) 여유
    c.setAsyncSendTimeout(15_000L);
    // 유휴 타임아웃(원하면 0=무한)
    // c.setMaxSessionIdleTimeout(0L);
    return c;
}


}
