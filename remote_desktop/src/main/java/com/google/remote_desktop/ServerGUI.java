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
import java.io.IOException;



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

    public ServerGUI() {
        // Set up the JFrame
        setTitle("Server Monitor");
        setSize(830, 528);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize components
        statusLabel = new JLabel("Server Status: Offline");
        portField = new JTextField("8080");
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
    }
   //
    private List<PrintWriter> clientOutputStreams = new ArrayList<>();

    private class AcceptClientsThread implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientInfo = "Client connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort();
                    updateConnectedClients(clientInfo);

                    
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
        private PrintWriter clientOut;

        public ClientCommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            
            //
            try {
                this.clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                clientOutputStreams.add(clientOut);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String message;
                while ((message = in.readLine()) != null) {
                    updateProcessInfo(message);
                    //
                    sendToAllClients(message);
                }
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //
        private void sendToAllClients(String message) {
            for (PrintWriter clientOut : clientOutputStreams) {
                clientOut.println(message);
            }
        }
        

		private void sendToTrackingClient(String message) {
			try {
	
		        String trackingClientIp = "127.0.0.1"; 
		        int trackingClientPort = 8080;

		        Socket trackingClientSocket = new Socket(trackingClientIp, trackingClientPort);
		        
		        
		        PrintWriter trackingClientOut = new PrintWriter(trackingClientSocket.getOutputStream(), true);
		        
		       
		        trackingClientOut.println(message);
		        
		        
		        trackingClientOut.close();
		        trackingClientSocket.close();
		    } catch (IOException e) {
		        e.printStackTrace();
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
