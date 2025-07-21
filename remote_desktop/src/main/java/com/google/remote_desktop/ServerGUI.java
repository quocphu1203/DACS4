package com.google.remote_desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import javax.swing.border.TitledBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;


public class ServerGUI extends JFrame {
    private JLabel statusLabel;
    private JTextField portField;
    private JTextArea connectedClientsArea;
    private JButton startServerButton;
    private JButton stopServerButton;
    private ServerSocket serverSocket;
    private JTextArea processInfoArea;
    private static boolean isLoggingEnabled = false;
    
    private FileWriter fileWriter;
    private DefaultListModel<String> clientListModel = new DefaultListModel<>();
    private JList<String> clientList = new JList<>(clientListModel);
    private Map<String, ClientHandler> clientHandlers = new HashMap<>();
    private Map<String, StringBuilder> clientLogs = new HashMap<>();
    private static final int IMAGE_PORT = 8181;
    private JButton toggleLoggingButton;

    public ServerGUI() {
        // Set up the JFrame
        setTitle("Server Monitor - Multi Client Control");
        setSize(900, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(new Color(245, 247, 250));

        // Initialize components
        statusLabel = new JLabel("Server Status: Offline");
        portField = new JTextField("8080");
        connectedClientsArea = new JTextArea(10, 30);
        startServerButton = new JButton("Start Server");
        stopServerButton = new JButton("Stop Server");

        // Set up the layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(245, 247, 250));

        // Top panel
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(245, 247, 250));
        topPanel.add(statusLabel);
        topPanel.add(new JLabel("Port:"));
        portField.setPreferredSize(new Dimension(70, 28));
        topPanel.add(portField);
        // Khởi tạo và add action cho toggleLoggingButton
        toggleLoggingButton = new JButton("Enable Logging");
        toggleLoggingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isLoggingEnabled = !isLoggingEnabled;
                if (isLoggingEnabled) {
                    toggleLoggingButton.setText("Disable Logging");
                } else {
                    toggleLoggingButton.setText("Enable Logging");
                }
            }
        });
        topPanel.add(toggleLoggingButton);
        JLabel titleLabel = new JLabel("SERVER MONITOR");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(33, 102, 172));
        topPanel.add(titleLabel);

        // Middle panel
        JPanel middlePanel = new JPanel(new GridLayout(1, 2, 12, 0));
        middlePanel.setBackground(new Color(245, 247, 250));

        // Process info area
        processInfoArea = new JTextArea();
        processInfoArea.setEditable(false);
        processInfoArea.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(33, 102, 172), 2),
            "Log/Process Info",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(33, 102, 172)
        ));
        processInfoArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        processInfoArea.setBackground(Color.WHITE);
        JScrollPane scrollPane_1 = new JScrollPane(processInfoArea);
        scrollPane_1.setBorder(new EmptyBorder(0, 0, 0, 0));
        middlePanel.add(scrollPane_1);

        // Client list area
        clientList.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(33, 102, 172), 2), "Danh sách client", TitledBorder.LEADING, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), new Color(33, 102, 172)));
        clientList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clientList.setBackground(Color.WHITE);
        JScrollPane clientListScroll = new JScrollPane(clientList);
        clientListScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        middlePanel.add(clientListScroll);

        // Bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 18, 12));
        bottomPanel.setBackground(new Color(245, 247, 250));
        startServerButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stopServerButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JButton btnCapture = new JButton("Chụp màn hình", new ImageIcon("src/main/java/com/google/remote_desktop/icon/photo-capture.png"));
        JButton btnShutdown = new JButton("Tắt máy", new ImageIcon("src/main/java/com/google/remote_desktop/icon/on-off-button.png"));
        JButton btnSendFile = new JButton("Gửi file", new ImageIcon("src/main/java/com/google/remote_desktop/icon/files.png"));
        JButton btnSendMsg = new JButton("Gửi tin nhắn", new ImageIcon("src/main/java/com/google/remote_desktop/icon/link.png"));
        btnCapture.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnShutdown.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSendFile.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSendMsg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bottomPanel.add(startServerButton);
        bottomPanel.add(stopServerButton);
        bottomPanel.add(btnCapture);
        bottomPanel.add(btnShutdown);
        bottomPanel.add(btnSendFile);
        bottomPanel.add(btnSendMsg);

        // Add panels to mainPanel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(middlePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // Add action listeners
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int port = Integer.parseInt(portField.getText());

                try {
                    serverSocket = new ServerSocket(port);
                    statusLabel.setText("Server Status: Online");
                    startServerButton.setEnabled(false);
                    stopServerButton.setEnabled(true);

                   
                    Thread acceptClientsThread = new Thread(new AcceptClientsThread());
                    acceptClientsThread.start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        stopServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                    
                    if (fileWriter != null) {
                        fileWriter.close();
                    }

                    statusLabel.setText("Server Status: Offline");
                    startServerButton.setEnabled(true);
                    stopServerButton.setEnabled(false);

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        btnCapture.addActionListener(e -> sendCommandToSelectedClient("Capture"));
        btnShutdown.addActionListener(e -> sendCommandToSelectedClient("Shutdown"));
        btnSendFile.addActionListener(e -> {
            String fileName = JOptionPane.showInputDialog(this, "Nhập tên file muốn gửi:");
            if (fileName != null && !fileName.isEmpty()) {
                sendCommandToSelectedClient("SendFile:" + fileName);
            }
        });
        btnSendMsg.addActionListener(e -> {
            String msg = JOptionPane.showInputDialog(this, "Nhập tin nhắn gửi tới client:");
            if (msg != null && !msg.isEmpty()) {
                sendCommandToSelectedClient("MSG:" + msg);
            }
        });
        clientList.addListSelectionListener(e -> {
            String selected = clientList.getSelectedValue();
            if (selected != null && clientLogs.containsKey(selected)) {
                processInfoArea.setText(clientLogs.get(selected).toString());
            } else {
                processInfoArea.setText("");
            }
        });
        startImageListener();
    }
   
    private List<PrintWriter> clientOutputStreams = new ArrayList<>();

    private void updateConnectedClients(String clientInfo, Socket clientSocket) {
        SwingUtilities.invokeLater(() -> {
            String key = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
            if (!clientListModel.contains(key)) {
                clientListModel.addElement(key);
            }
        });
    }

    private void removeClient(Socket clientSocket) {
        SwingUtilities.invokeLater(() -> {
            String key = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
            clientListModel.removeElement(key);
            clientHandlers.remove(key);
            clientLogs.remove(key);
        });
    }

    private void sendCommandToSelectedClient(String command) {
        String selected = clientList.getSelectedValue();
        if (selected != null && clientHandlers.containsKey(selected)) {
            clientHandlers.get(selected).sendCommand(command);
        } else {
            JOptionPane.showMessageDialog(this, "Hãy chọn một client để gửi lệnh!", "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void startImageListener() {
        Thread imageListenerThread = new Thread(() -> {
            try (ServerSocket imageServerSocket = new ServerSocket(IMAGE_PORT)) {
                while (true) {
                    Socket imageSocket = imageServerSocket.accept();
                    DataInputStream dis = new DataInputStream(imageSocket.getInputStream());
                    int imageSize = dis.readInt();
                    byte[] imageData = new byte[imageSize];
                    dis.readFully(imageData);
                    imageSocket.close();

                    // Lưu file ảnh
                    File imgFile = new File("screenshot_" + System.currentTimeMillis() + ".png");
                    try (FileOutputStream fos = new FileOutputStream(imgFile)) {
                        fos.write(imageData);
                    }

                    // Hiển thị ảnh lên giao diện
                    SwingUtilities.invokeLater(() -> {
                        ImageIcon icon = new ImageIcon(imageData);
                        JLabel label = new JLabel(icon);
                        JOptionPane.showMessageDialog(this, label, "Ảnh chụp màn hình", JOptionPane.PLAIN_MESSAGE);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        imageListenerThread.setDaemon(true);
        imageListenerThread.start();
    }

    private class AcceptClientsThread implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String key = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
                    updateConnectedClients(key, clientSocket);
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clientHandlers.put(key, handler);
                    Thread clientThread = new Thread(handler);
                    clientThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter clientOut;
        private BufferedReader in;
        private String clientKey;
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.clientKey = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
            try {
                this.clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void sendCommand(String command) {
            clientOut.println(command);
        }
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    updateProcessInfoForClient(clientKey, message);
                }
                clientSocket.close();
                removeClient(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
                removeClient(clientSocket);
            }
        }
    }

    private void updateProcessInfo(String info) {
        SwingUtilities.invokeLater(() -> {
            processInfoArea.append(info + "\n");
            if (isLoggingEnabled) {
                logProcessInfo(info);
            }
        });
    }

    private void updateProcessInfoForClient(String clientKey, String info) {
        SwingUtilities.invokeLater(() -> {
            clientLogs.putIfAbsent(clientKey, new StringBuilder());
            clientLogs.get(clientKey).append(info).append("\n");
            // Nếu client đang được chọn thì cập nhật processInfoArea
            String selected = clientList.getSelectedValue();
            if (selected != null && selected.equals(clientKey)) {
                processInfoArea.setText(clientLogs.get(clientKey).toString());
            }
        });
    }

    private void logProcessInfo(String info) {
        try {
            if (fileWriter == null) {
                fileWriter = new FileWriter("info.txt", true);
            }
            fileWriter.write(info + "\n");
            fileWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateConnectedClients(String clientInfo) {
        SwingUtilities.invokeLater(() -> {
            connectedClientsArea.append(clientInfo + "\n");
        });
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        	ServerGUI serverMonitor = new ServerGUI();
            serverMonitor.setVisible(true);           
        });
    }
    

}
