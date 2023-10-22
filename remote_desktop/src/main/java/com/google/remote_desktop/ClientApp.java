package com.google.remote_desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class ClientApp extends JFrame {
    private JTextField serverIpField;
    private JTextField serverPortField;
    private JButton connectButton;
    private JTextArea chatTextArea;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    

    public ClientApp() {
        // Set up the JFrame
        setTitle("Client ");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize components
        serverIpField = new JTextField("127.0.0.1");
        serverPortField = new JTextField("8080");
        connectButton = new JButton("Connect to Server");
        chatTextArea = new JTextArea(10, 30);

        // Set up the layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Server IP:"));
        topPanel.add(serverIpField);
        topPanel.add(new JLabel("Server Port:"));
        topPanel.add(serverPortField);
        topPanel.add(connectButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(chatTextArea), BorderLayout.CENTER);

        add(mainPanel);

        // Add action listener for Connect button
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });
        
//        Timer processInfoTimer = new Timer(5000, new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                sendProcessInfoToServer();
//            }
//        });
//        processInfoTimer.start();
    }
    
//    private void sendProcessInfoToServer() {
//        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
//        String processInfo = String.format("Process ID: %s, Process Name: %s",
//                runtimeMXBean.getName(), runtimeMXBean.getVmName());
//        out.println(processInfo); // Send process information to the server
//    }
    
//    private void sendProcessInfoToServer() {
//        String activeApp = getActiveApplication();
//        out.println(activeApp); // Send active application information to the server
//    }

//    private void connectToServer() {
//        String serverIp = serverIpField.getText();
//        int serverPort = Integer.parseInt(serverPortField.getText());
//
//        try {
//            socket = new Socket(serverIp, serverPort);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//            chatTextArea.append("Connected to the server.\n");
//            
//            Thread processInfoThread = new Thread(new ProcessInfoSender());
//            processInfoThread.start();
//
//        } catch (IOException e) {
//            chatTextArea.append("Connection to the server failed.\n");
//            e.printStackTrace();
//        }
//    }
    
    private void connectToServer() {
        String serverIp = serverIpField.getText();
        int serverPort = Integer.parseInt(serverPortField.getText());

        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);

            chatTextArea.append("Connected to the server.\n");
            Thread processInfoThread = new Thread(new ProcessInfoSender());
            processInfoThread.start();

        } catch (IOException e) {
            chatTextArea.append("Connection to the server failed.\n");
            e.printStackTrace();
        }
    }
    
    private String getActiveApplication() {
        User32 user32 = User32.INSTANCE;
        char[] windowText = new char[512];

        WinDef.HWND hwnd = user32.GetForegroundWindow();
        user32.GetWindowText(hwnd, windowText, 512);

        return Native.toString(windowText);
    }
    
    
    private class ProcessInfoSender implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("tasklist");
                    Process process = processBuilder.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    List<String> processInfo = new ArrayList<>();

                    while ((line = reader.readLine()) != null) {
                    	String[] parts = line.split("\\s+");
                    	if (parts.length > 0) {
                    		processInfo.add(parts[0]);
                        }
//                        processInfo.add(line);
                    }

                    reader.close();
                    out.println(String.join("\n", processInfo));
                    Thread.sleep(5000);  
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientApp clientApp = new ClientApp();
            clientApp.setVisible(true);
        });
    }
}


