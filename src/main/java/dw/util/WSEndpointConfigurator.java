package dw.util;

import javax.websocket.server.ServerEndpointConfig;

public class WSEndpointConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(javax.websocket.server.ServerEndpointConfig sec,
                                 javax.websocket.server.HandshakeRequest request,
                                 javax.websocket.HandshakeResponse response) {
        // JWT validation is handled in WSServPoint onOpen
    }
}
