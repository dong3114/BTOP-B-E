package com.aiaca.btop.config;

import com.aiaca.btop.ws.EchoWsHandler;
import com.aiaca.btop.ws.SttWsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Bean public EchoWsHandler echoWsHandler() { return new EchoWsHandler(); }
  @Bean public SttWsHandler  sttWsHandler()  { return new SttWsHandler();  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(echoWsHandler(), "/ws/echo")
            .setAllowedOriginPatterns("*");   // 개발용 전체 허용
    registry.addHandler(sttWsHandler(), "/ws/stt")
            .setAllowedOriginPatterns("*");
  }
}
