package com.google.remote_desktop;

//ClientApp.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class test1 extends JFrame {
 private JTextField serverIpField;
 private JTextField serverPortField;
 private JButton connectButton;
 private JTextArea chatTextArea;
 private Socket socket;
 private PrintWriter out;
 private BufferedReader in;
 

 public test1() {
     // Set up the JFrame
     setTitle("Client Application");
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
 }
 
 

 public void connectToServer() {
     String serverIp = serverIpField.getText();
     int serverPort = Integer.parseInt(serverPortField.getText());

     try {
         socket = new Socket(serverIp, serverPort);
         out = new PrintWriter(socket.getOutputStream(), true);
         in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

         chatTextArea.append("Connected to the server.\n");

         Thread processInfoThread = new Thread(new ProcessInfoSender());
         processInfoThread.start();

     } catch (IOException e) {
         chatTextArea.append("Connection to the server failed.\n");
         e.printStackTrace();
     }
 }
 
 

 private class ProcessInfoSender implements Runnable {
	 
     @Override
     public void run() {
         while (!Thread.interrupted()) {

        		 try {
                     String activeProcess = getActiveProcess();
                     out.println(activeProcess);
                     Thread.sleep(5000);
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }
			
         }
     }
	 
     
     private String lastProcessName = "";

     private String getActiveProcess() {
         User32 user32 = User32.INSTANCE;
         char[] windowText = new char[512];
         WinDef.HWND hwnd = user32.GetForegroundWindow();
         user32.GetWindowText(hwnd, windowText, 512);

         String processName = Native.toString(windowText);
         String timestamp = getTimestamp();

         if (!processName.equals(lastProcessName)) {
            
             String endStatus = timestamp + ": " + "End - " + lastProcessName;
             lastProcessName = processName;

             String beginStatus = timestamp + ": " + "Begin - " + processName;
             return endStatus + "\n" + beginStatus;
         }

         return timestamp + ": " + "Running - " + processName;
     }

     
     private String getTimestamp() {
         java.util.Date date = new java.util.Date();
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         return sdf.format(date);
     }
 }

 public static void main(String[] args) {
     SwingUtilities.invokeLater(() -> {
    	 test1 clientApp = new test1();
         clientApp.setVisible(true);
     });
 }
 
}




