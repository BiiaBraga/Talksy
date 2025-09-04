package com.example.talksy;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class TalksyChat {
    private final String brokerUrl;
    private final String username;

    private Connection connection;
    private Session session;
    private MessageProducer publicProducer;
    private MessageConsumer publicConsumer;
    private MessageConsumer privateConsumer;
    private Topic publicTopic;

    private BiConsumer<String, Boolean> onMessage;
    private Runnable onPresenceUpdate;

    private static final String PUBLIC_TOPIC = "TALKSY.PUBLIC";
    private static final String PRIVATE_PREFIX = "TALKSY.PRIVATE.";
    private static final String PRESENCE_TYPE = "presence";

    // tipos de presença
    private static final String PRESENCE_JOIN = "join";
    private static final String PRESENCE_LEAVE = "leave";
    private static final String PRESENCE_SYNC_REQUEST = "sync_request";
    private static final String PRESENCE_SYNC_RESPONSE = "sync_response";

    private static final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public TalksyChat(String brokerUrl, String username) {
        this.brokerUrl = brokerUrl;
        this.username = username;
    }

    public void setOnMessage(BiConsumer<String, Boolean> onMessage) {
        this.onMessage = onMessage;
    }

    public void setOnPresenceUpdate(Runnable onPresenceUpdate) {
        this.onPresenceUpdate = onPresenceUpdate;
    }

    public Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    public void connect() throws JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(brokerUrl);
        connection = cf.createConnection();
        connection.setClientID("talksy-" + username + "-" + UUID.randomUUID());
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        publicTopic = session.createTopic(PUBLIC_TOPIC);
        Queue myPrivateQueue = session.createQueue(PRIVATE_PREFIX + username);

        publicProducer = session.createProducer(publicTopic);
        publicProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        publicConsumer = session.createConsumer(publicTopic);
        publicConsumer.setMessageListener(msg -> handleIncoming(msg, false));

        privateConsumer = session.createConsumer(myPrivateQueue);
        privateConsumer.setMessageListener(msg -> handleIncoming(msg, true));

        connection.start();

        // Anunciar presença e pedir lista
        sendPresence(PRESENCE_JOIN);
        sendPresence(PRESENCE_SYNC_REQUEST);
    }

    private void handleIncoming(Message message, boolean isPrivate) {
        try {
            if (message instanceof TextMessage) {
                TextMessage tm = (TextMessage) message;
                String type = tm.getStringProperty("type");
                String sender = tm.getStringProperty("sender");

                if (PRESENCE_TYPE.equals(type)) {
                    String action = tm.getText();

                    switch (action) {
                        case PRESENCE_JOIN:
                            onlineUsers.add(sender);
                            break;

                        case PRESENCE_LEAVE:
                            onlineUsers.remove(sender);
                            break;

                        case PRESENCE_SYNC_REQUEST:
                            // alguém pediu sincronização -> mando minha presença de volta
                            if (!sender.equals(username)) {
                                sendPresence(PRESENCE_SYNC_RESPONSE);
                            }
                            break;

                        case PRESENCE_SYNC_RESPONSE:
                            // alguém respondeu que está online
                            onlineUsers.add(sender);
                            break;
                    }

                    if (onPresenceUpdate != null) onPresenceUpdate.run();
                } else {
                    // Mensagem normal
                    String text = tm.getText();
                    String prefix = isPrivate ? "(privado) " : "";
                    String line = String.format("%s%s: %s", prefix, sender, text);
                    if (onMessage != null) onMessage.accept(line, isPrivate);
                }
            }
        } catch (JMSException e) {
            if (onMessage != null) onMessage.accept("Erro: " + e.getMessage(), isPrivate);
        }
    }

    /** Envia mensagem pública para o tópico */
    public void sendPublic(String text) throws JMSException {
        TextMessage msg = session.createTextMessage(text);
        msg.setStringProperty("sender", username);
        publicProducer.send(msg);
    }

    /** Envia mensagem privada para uma fila específica */
    public void sendPrivate(String toUser, String text) throws JMSException {
        Queue targetQueue = session.createQueue(PRIVATE_PREFIX + toUser);
        MessageProducer p = session.createProducer(targetQueue);
        p.setDeliveryMode(DeliveryMode.PERSISTENT);
        TextMessage msg = session.createTextMessage(text);
        msg.setStringProperty("sender", username);
        p.send(msg);
        p.close();
    }

    /** Envia presença (join/leave/sync) */
    public void sendPresence(String action) throws JMSException {
        TextMessage msg = session.createTextMessage(action);
        msg.setStringProperty("type", PRESENCE_TYPE);
        msg.setStringProperty("sender", username);
        publicProducer.send(msg);
    }

    public void close() {
        try { sendPresence(PRESENCE_LEAVE); } catch (Exception ignored) {}
        try { if (publicConsumer != null) publicConsumer.close(); } catch (Exception ignored) {}
        try { if (privateConsumer != null) privateConsumer.close(); } catch (Exception ignored) {}
        try { if (publicProducer != null) publicProducer.close(); } catch (Exception ignored) {}
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (connection != null) connection.close(); } catch (Exception ignored) {}
    }
}