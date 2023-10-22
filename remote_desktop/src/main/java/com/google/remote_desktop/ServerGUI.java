package com.google.remote_desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerGUI extends JFrame {
    private JLabel statusLabel;
    private JTextField portField;
    private JTextField ipField;
    private JTextArea connectedClientsArea;
    private JButton startServerButton;
    private JButton stopServerButton;
    private ServerSocket serverSocket;
    private JTextArea processInfoArea;

    public ServerGUI() {
        // Set up the JFrame
        setTitle("Server Monitor");
        setSize(830, 528);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize components
        statusLabel = new JLabel("Server Status: Offline");
        portField = new JTextField("8080");
        ipField = new JTextField("127.0.0.1");
        connectedClientsArea = new JTextArea(10, 30);
        startServerButton = new JButton("Start Server");
        stopServerButton = new JButton("Stop Server");

        // Set up the layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(statusLabel);
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(new JLabel("IP:"));
        topPanel.add(ipField);

        JPanel middlePanel = new JPanel();
        middlePanel.setLayout(new GridLayout(0, 2, 0, 0));
        
        JScrollPane scrollPane_1 = new JScrollPane();
        middlePanel.add(scrollPane_1);
        
        processInfoArea = new JTextArea();
        scrollPane_1.setViewportView(processInfoArea);
        JScrollPane scrollPane = new JScrollPane(connectedClientsArea);
        middlePanel.add(scrollPane);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(startServerButton);
        bottomPanel.add(stopServerButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(middlePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        


        getContentPane().add(mainPanel);

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

                    // Create a thread for accepting client connections
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
                    serverSocket.close();
                    statusLabel.setText("Server Status: Offline");
                    startServerButton.setEnabled(true);
                    stopServerButton.setEnabled(false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    
    private class AcceptClientsThread implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientInfo = "Client connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort();
                    updateConnectedClients(clientInfo);

                    // Create a thread to handle client communication
                    Thread clientThread = new Thread(new ClientCommunicationThread(clientSocket));
                    clientThread.start();
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    private class ClientCommunicationThread implements Runnable {
        private Socket clientSocket;

        public ClientCommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String message;
                while ((message = in.readLine()) != null) {
                	updateProcessInfo(message);
                }

                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void updateProcessInfo(String info) {
        SwingUtilities.invokeLater(() -> {
            processInfoArea.append(info + "\n");
        });
    }
    
    private void updateConnectedClients(String clientInfo) {
        SwingUtilities.invokeLater(() -> {
            connectedClientsArea.append(clientInfo + "\n");
        });
    }
    
    private void updateActiveApp(String appInfo) {
        SwingUtilities.invokeLater(() -> {
        	processInfoArea.setText(appInfo);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        	ServerGUI serverMonitor = new ServerGUI();
            serverMonitor.setVisible(true);
        });
    }
}
