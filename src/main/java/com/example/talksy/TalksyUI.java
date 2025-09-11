package com.example.talksy;

import javax.jms.JMSException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TalksyUI extends JFrame {
    private final TalksyChat chat;
    private final JTextPane chatPane;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JButton privateButton;
    private final DefaultListModel<String> userListModel;
    private final JList<String> userList;
    private final Map<String, Color> userColors = new HashMap<>();

    private final String myUsername;

    private final HTMLEditorKit kit = new HTMLEditorKit();
    private final HTMLDocument doc = new HTMLDocument();

    public TalksyUI() {
        // ===== LOGIN =====
        myUsername = JOptionPane.showInputDialog(
                null,
                "Digite seu nome de usuário:",
                "Login no Talksy",
                JOptionPane.PLAIN_MESSAGE
        );

        if (myUsername == null || myUsername.trim().isEmpty()) {
            System.exit(0);
        }

        String serverIp = JOptionPane.showInputDialog(
                null,
                "Digite o IP do servidor ActiveMQ (ex: 192.168.0.10):",
                "Conectar ao Talksy",
                JOptionPane.PLAIN_MESSAGE
        );

        if (serverIp == null || serverIp.trim().isEmpty()) {
            System.exit(0);
        }

        setTitle("💬 Talksy Chat - " + myUsername);

        // ===== BACKEND =====
        chat = new TalksyChat("tcp://" + serverIp + ":61616", myUsername);

        // ===== ÁREA DE CHAT =====
        chatPane = new JTextPane();
        chatPane.setEditorKit(kit);
        chatPane.setDocument(doc);
        chatPane.setEditable(false);
        chatPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane chatScroll = new JScrollPane(chatPane);

        // ===== INPUT =====
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        sendButton = new JButton("Enviar");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setBackground(new Color(25, 118, 210));
        sendButton.setForeground(Color.WHITE);

        privateButton = new JButton("Privado");
        privateButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        privateButton.setBackground(new Color(255, 152, 0));
        privateButton.setForeground(Color.WHITE);

        sendButton.setFocusPainted(false);
        privateButton.setFocusPainted(false);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(privateButton);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // ===== LISTA DE USUÁRIOS =====
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userList.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(200, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("👥 Usuários Online"));

        // ===== LAYOUT PRINCIPAL =====
        setLayout(new BorderLayout());
        add(chatScroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(userScroll, BorderLayout.EAST);

        // ===== EVENTOS =====
        sendButton.addActionListener(e -> sendMessage(false));
        privateButton.addActionListener(e -> sendMessage(true));
        inputField.addActionListener(e -> sendMessage(false));

        // ===== CALLBACKS =====
        chat.setOnMessage((sender, text, isPrivate) ->
                SwingUtilities.invokeLater(() -> appendMessage(sender, text, isPrivate)));

        chat.setOnPresenceUpdate(() ->
                SwingUtilities.invokeLater(() -> {
                    userListModel.clear();
                    for (String u : chat.getOnlineUsers()) {
                        userListModel.addElement(u);
                    }
                }));

        // ===== CONEXÃO =====
        try {
            chat.connect();
            appendSystemMessage("🚀 Bem-vindo ao Talksy");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao conectar no servidor: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            System.exit(1);
        }

        // ===== FECHAR CONEXÃO AO SAIR =====
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                chat.close();
            }
        });

        // ===== CONFIG JANELA =====
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void sendMessage(boolean isPrivate) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        try {
            if (isPrivate) {
                String targetUser = userList.getSelectedValue();
                if (targetUser == null) {
                    appendSystemMessage("⚠️ Selecione um usuário na lista para enviar mensagem privada.");
                    return;
                }

                appendMessage("Para " + targetUser, text, true);
                chat.sendPrivate(targetUser, text);

            } else {
                appendMessage(myUsername, text, false);
                chat.sendPublic(text);
            }

            inputField.setText("");

        } catch (JMSException ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao enviar mensagem: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void appendSystemMessage(String message) {
        try {
            kit.insertHTML(doc, doc.getLength(),
                    String.format("<p style='color:gray; text-align:center;'>%s</p>", message),
                    0, 0, null);
            chatPane.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendMessage(String sender, String message, boolean isPrivate) {
        try {
            String color = colorForUser(sender);
            String align = sender.equals(myUsername) ? "right" : "left";
            String bgColor;
            String textColor;
            String privateTag = "";

            if ("sistema".equalsIgnoreCase(sender)) {
                appendSystemMessage("🔔 " + message);
                return;
            }

            if (sender.equals(myUsername)) {
                bgColor = "#e3edf6ff";   // azul para mim
                textColor = "black";
            } else {
                bgColor = "#f4dee6ff";   // cinza para outros
                textColor = "black";
            }

            if (isPrivate) {
                privateTag = "<i style='font-size:10px; color:#555;'> (privado)</i>";
                bgColor = "#f6f3daff"; // fundo amarelo
            }

            kit.insertHTML(doc, doc.getLength(),
                    String.format(
                            "<div style='text-align:%s; margin:6px 0;'>"
                                    + "<div style='display:inline-block; background:%s; color:%s; padding:10px 14px; "
                                    + "border-radius:12px; max-width:60%%; font-size:14px; "
                                    + "box-shadow:0 2px 5px rgba(0,0,0,0.15); word-wrap: break-word;'>"
                                    + "<b style='color:%s;'>%s</b>%s<br>%s</div></div>",
                            align, bgColor, textColor, color, sender, privateTag, message
                    ),
                    0, 0, null);

            chatPane.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String colorForUser(String user) {
        if (!userColors.containsKey(user)) {
            Random rand = new Random(user.hashCode());
            Color c = new Color(100 + rand.nextInt(155),
                    100 + rand.nextInt(155),
                    100 + rand.nextInt(155));
            userColors.put(user, c);
        }
        Color c = userColors.get(user);
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TalksyUI().setVisible(true));
    }
}
