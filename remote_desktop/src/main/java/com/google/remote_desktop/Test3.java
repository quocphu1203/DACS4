package com.google.remote_desktop;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Test3 extends JFrame {
    private JTextArea processInfoArea;
    private JTextField serverIpField;
    private JTextField serverPortField;

    public Test3() {
        setTitle("Client to Receive Process Info");
        setSize(418, 656);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        processInfoArea = new JTextArea();
        processInfoArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(processInfoArea);
        getContentPane().setLayout(new BorderLayout(0, 0));

        serverIpField = new JTextField("127.0.0.1");
        serverPortField = new JTextField("8080");
        JButton connectButton = new JButton("Connect to Server");

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Server IP:"));
        topPanel.add(serverIpField);
        topPanel.add(new JLabel("Server Port:"));
        topPanel.add(serverPortField);
        topPanel.add(connectButton);

        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(scrollPane);
        
        JPanel sidePanel = new JPanel();
        getContentPane().add(sidePanel, BorderLayout.SOUTH);
        
        JButton btnLog = new JButton("Log");
        sidePanel.add(btnLog);
        
        JButton btnCapture = new JButton("Capture");
        sidePanel.add(btnCapture);
        
        JButton btnControl = new JButton("Control");
        sidePanel.add(btnControl);

        connectButton.addActionListener(e -> {

        	String serverIp = serverIpField.getText();
            int serverPort = Integer.parseInt(serverPortField.getText());


            Thread connectionThread = new Thread(() -> connectToServer(serverIp, serverPort));
            connectionThread.start();
        });
    }

    private void connectToServer(String serverIp, int serverPort) {
    	  try {
              Socket socket = new Socket(serverIp, serverPort);
              BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

              String message;
              while ((message = in.readLine()) != null) {
                  processInfoArea.append(message + "\n");

              }

              socket.close();
          } catch (IOException e) {
              e.printStackTrace();
          }
    }
    


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Test3 clientToReceive = new Test3();
            clientToReceive.setVisible(true);
        });
    }
}
