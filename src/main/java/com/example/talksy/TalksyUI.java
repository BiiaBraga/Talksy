package com.example.talksy;

import javax.jms.JMSException;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TalksyUI extends JFrame {
    private JTextPane chatPane;
    private JTextField inputField;
    private JTextField userField;
    private JTextField brokerField;
    private JButton connectBtn, sendBtn;
    private JCheckBox privateCheck;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    private TalksyChat jms;
    private final Map<String, Color> userColors = new HashMap<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public TalksyUI() {
        super("Talksy");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Usuário:"));
        userField = new JTextField(System.getProperty("user.name", "user"), 10);
        top.add(userField);
        top.add(new JLabel("Broker:"));
        brokerField = new JTextField("tcp://localhost:61616", 20);
        top.add(brokerField);
        connectBtn = new JButton("Conectar");
        top.add(connectBtn);

        chatPane = new JTextPane();
        chatPane.setEditable(false);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));

        JScrollPane chatScroll = new JScrollPane(chatPane);

        JPanel bottom = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("Enviar");
        privateCheck = new JCheckBox("Mensagem Privada");
        JPanel bottomRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomRight.add(privateCheck);
        bottomRight.add(sendBtn);

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(bottomRight, BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userScroll, chatScroll);
        split.setDividerLocation(150);

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);

        connectBtn.addActionListener(e -> onConnect());
        sendBtn.addActionListener(e -> onSend());
        inputField.addActionListener(e -> onSend());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (jms != null) {
                    jms.close();
                }
            }
        });
    }

    private void onConnect() {
        String user = userField.getText().trim();
        String broker = brokerField.getText().trim();
        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o usuário");
            return;
        }
        connectBtn.setEnabled(false);
        try {
            jms = new TalksyChat(broker, user);
            jms.setOnMessage((line, isPriv) -> SwingUtilities.invokeLater(() -> {
                appendMessage(line, user);
            }));
            jms.setOnPresenceUpdate(() -> SwingUtilities.invokeLater(this::updateUserList));
            jms.connect();
            appendSystem("Conectado como '" + user + "' em " + broker);
        } catch (Exception ex) {
            connectBtn.setEnabled(true);
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }

    private void onSend() {
        if (jms == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");

        try {
            if (privateCheck.isSelected() && !userList.isSelectionEmpty()) {
                String to = userList.getSelectedValue();
                jms.sendPrivate(to, text);
                appendMessage("(privado) Você → " + to + ": " + text, userField.getText());
            } else {
                jms.sendPublic(text);
            }
        } catch (JMSException e) {
            JOptionPane.showMessageDialog(this, "Erro ao enviar: " + e.getMessage());
        }
    }

    private void updateUserList() {
        userListModel.clear();
        for (String u : jms.getOnlineUsers()) {
            userListModel.addElement(u);
        }
        setTitle("Talksy [" + jms.getOnlineUsers().size() + " conectados]");
    }

    private void appendMessage(String line, String sender) {
        StyledDocument doc = chatPane.getStyledDocument();
        Style style = doc.addStyle("user", null);
        Color c = userColors.computeIfAbsent(sender, k -> randomColor());
        StyleConstants.setForeground(style, c);
        StyleConstants.setBold(style, true);

        try {
            String time = timeFormat.format(new Date());
            doc.insertString(doc.getLength(), "[" + time + "] " + line + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendSystem(String msg) {
        try {
            StyledDocument doc = chatPane.getStyledDocument();
            Style style = doc.addStyle("sys", null);
            StyleConstants.setForeground(style, Color.GRAY);
            StyleConstants.setItalic(style, true);
            doc.insertString(doc.getLength(), "[sistema] " + msg + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private Color randomColor() {
        Random r = new Random();
        return new Color(r.nextInt(200), r.nextInt(200), r.nextInt(200));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TalksyUI().setVisible(true));
    }
}