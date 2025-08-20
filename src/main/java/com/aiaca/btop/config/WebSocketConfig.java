package com.aiaca.btop.config;

import com.aiaca.btop.ws.EchoWsHandler;
import com.aiaca.btop.ws.SttWsHandler;
import com.aiaca.btop.stt.MockSttService;
import com.aiaca.btop.stt.SttService;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Bean
  public EchoWsHandler echoWsHandler() {
    return new EchoWsHandler();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public SttService sttService() {
    return new MockSttService(); 
  }

 @Bean
public SttWsHandler sttWsHandler(org.springframework.beans.factory.ObjectFactory<com.aiaca.btop.stt.SttService> sttFactory) {
  return new SttWsHandler(sttFactory);
}

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(echoWsHandler(), "/ws/echo").setAllowedOriginPatterns("*");
    registry.addHandler(sttWsHandler(null),  "/ws/stt" ).setAllowedOriginPatterns("*");
  }
}
