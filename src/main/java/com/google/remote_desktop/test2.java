package com.google.remote_desktop;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class test2 extends JFrame {
    private JLabel statusLabel;
    private JTextField portField;
    private JTextArea connectedClientsArea;
    private JButton startServerButton;
    private JButton stopServerButton;
    
    private ServerSocket serverSocket;
    private ServerSocket serverImageSocket;
    private JTextArea processInfoArea;
    private static boolean isLoggingEnabled = false;
    private FileWriter fileWriter;
    private boolean isServerRunning = false;
    private final List<DataOutputStream> clientOutputStreams = new ArrayList<>();
    private final List<DataOutputStream> clientImageOutputStreams = new ArrayList<>();
    private final List<ClientCommunicationThread> clientThreads = new ArrayList<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public test2() {
        setTitle("Server 2 SSS   sadfsdfasSS");
        setSize(830, 528);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        statusLabel = new JLabel("Server Status: Offline");
        portField = new JTextField("8080");
        connectedClientsArea = new JTextArea(10, 30);
        startServerButton = new JButton("Start Server");
        stopServerButton = new JButton("Stop Server");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(statusLabel);
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);

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

        JButton toggleLoggingButton = new JButton("Enable Logging");
        topPanel.add(toggleLoggingButton);
        toggleLoggingButton.addActionListener(e -> {
            isLoggingEnabled = !isLoggingEnabled;
            if (isLoggingEnabled) {
                toggleLoggingButton.setText("Disable Logging");
            } else {
                toggleLoggingButton.setText("Enable Logging");
            }
        });

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(middlePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);

        startServerButton.addActionListener(e -> {
            if (isServerRunning) {
                JOptionPane.showMessageDialog(this, "Server is already running.");
                return;
            }

            int port = Integer.parseInt(portField.getText());
            int imagePort = 8181;

            try {
                serverSocket = new ServerSocket(port);
                serverImageSocket = new ServerSocket(imagePort);

                statusLabel.setText("Server Status: Online");
                
                startServerButton.setEnabled(false);
                stopServerButton.setEnabled(true);
                
                isServerRunning = true;

                executorService.execute(new AcceptClientsThread());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        stopServerButton.addActionListener(e -> {
            if (!isServerRunning) {
                JOptionPane.showMessageDialog(this, "Server is not running.");
                return;
            }

            try {
                if (serverSocket != null && serverImageSocket != null) {
                    serverSocket.close();
                    serverImageSocket.close();
                }

                if (fileWriter != null) {
                    fileWriter.close();
                }

                stopImageReceivingThreads();

                statusLabel.setText("Server Status: Offline");
                
                startServerButton.setEnabled(true);
                stopServerButton.setEnabled(false);
                
                isServerRunning = false;

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private class AcceptClientsThread implements Runnable {
        @Override
        public void run() {
            while (isServerRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Socket clientImageSocket = serverImageSocket.accept();
                    
                    String clientInfo = "Client connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort();
                    updateConnectedClients(clientInfo);

                    ClientCommunicationThread clientThread = new ClientCommunicationThread(clientSocket, clientImageSocket);
                    
                    clientThreads.add(clientThread);
                    executorService.execute(clientThread);

                } catch (IOException e) {
                     e.printStackTrace();
                     SwingUtilities.invokeLater(() -> updateProcessInfo("Server closed"));
                }
            }
        }
    }

    private class ClientCommunicationThread implements Runnable {
        private Socket clientSocket;
        private Socket clientImageSocket;
        private DataOutputStream clientOut;
        private DataOutputStream clientOutImage;
        private volatile boolean isRunning = true;

        public ClientCommunicationThread(Socket clientSocket, Socket clientImageSocket) {
            this.clientSocket = clientSocket;
            this.clientImageSocket = clientImageSocket;
            try {
                this.clientOut = new DataOutputStream(clientSocket.getOutputStream());
                this.clientOutImage = new DataOutputStream(clientImageSocket.getOutputStream());
                synchronized (clientOutputStreams) {
                    clientOutputStreams.add(clientOut);
                }
                synchronized (clientImageOutputStreams) {
                    clientImageOutputStreams.add(clientOutImage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            executorService.execute(this::receiveAndSendProcessInfo);
            executorService.execute(this::receiveAndSendScreenImage);

            while (isRunning) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            closeSockets();
        }

        public void stopThread() {
            isRunning = false;
        }

        private void closeSockets() {
            try {
                clientSocket.close();
                clientImageSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void receiveAndSendScreenImage() {
            try {
                DataInputStream imageDis = new DataInputStream(clientImageSocket.getInputStream());
                int imageSize;
                byte[] imageData;
                while (isRunning) {
                    imageSize = imageDis.readInt();
                    imageData = new byte[imageSize];
                    imageDis.readFully(imageData);
                    sendImageToAllClients(imageData);
                    System.out.println("Chup man hinh");
                }
            } catch (IOException e) {
                      
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> updateProcessInfo("ClientTest closed"));
                stopThread();
            }
        }

        private void receiveAndSendProcessInfo() {
            try {
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                String message;
                while (isRunning) {
                    message = dis.readUTF();
                    updateProcessInfo(message);
                    
                    if (message.equals("Shutdown")) {
                        sendToTest1("Shutdown");
                    }
                    
                    sendToAllClients(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    
    private void sendToAllClients(String message) {
        synchronized (clientOutputStreams) {
            for (DataOutputStream clientOut : clientOutputStreams) {
                try {
                    clientOut.writeUTF(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendImageToAllClients(byte[] imageData) {
        synchronized (clientImageOutputStreams) {
            for (DataOutputStream clientOutImage : clientImageOutputStreams) {
                try {
                    clientOutImage.writeInt(imageData.length);
                    clientOutImage.write(imageData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void updateProcessInfo(String info) {
        SwingUtilities.invokeLater(() -> {
            processInfoArea.append(info + "\n");
            if (isLoggingEnabled) {
                logProcessInfo(info);
            }
        });
    }

    private synchronized void logProcessInfo(String info) {
        try {
            if (fileWriter == null) {
                fileWriter = new FileWriter("info.txt", false);
            }
            fileWriter.write(info + "\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateConnectedClients(String clientInfo) {
        SwingUtilities.invokeLater(() -> {
            connectedClientsArea.append(clientInfo + "\n");
        });
    }

    private void stopImageReceivingThreads() {
        for (ClientCommunicationThread clientThread : clientThreads) {
            clientThread.stopThread();
        }
        executorService.shutdownNow();
        synchronized (clientImageOutputStreams) {
            for (DataOutputStream clientOutImage : clientImageOutputStreams) {
                try {
                    clientOutImage.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            clientImageOutputStreams.clear();
        }
    }
    
    //shutdown    
    private void sendToTest1(String message) {
        synchronized (clientOutputStreams) {
            if (!clientOutputStreams.isEmpty()) {
                try {
                    DataOutputStream test1OutputStream = clientOutputStreams.get(0);
                    test1OutputStream.writeUTF(message);
                    test1OutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        	test2 serverMonitor = new test2();
            serverMonitor.setVisible(true);
        });
    }
}