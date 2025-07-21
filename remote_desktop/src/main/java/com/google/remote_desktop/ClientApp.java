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

import java.io.IOException;

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

         // Thread lắng nghe tin nhắn và lệnh từ server
         Thread listenThread = new Thread(() -> {
             try {
                 String line;
                 while ((line = in.readLine()) != null) {
                     if (line.startsWith("MSG:")) {
                         String msg = line.substring(4);
                         chatTextArea.append("Tin nhắn từ server: " + msg + "\n");
                     } else if (line.equals("Capture")) {
                         captureScreenAndSend(serverIp);
                     }
                     // Có thể xử lý thêm các lệnh khác ở đây nếu muốn
                 }
             } catch (IOException e) {
                 chatTextArea.append("Mất kết nối tới server.\n");
             }
         });
         listenThread.start();

     } catch (IOException e) {
         chatTextArea.append("Connection to the server failed.\n");
         e.printStackTrace();
     }
 }

 // Hàm chụp màn hình và gửi về server qua port 8181
 private void captureScreenAndSend(String serverIp) {
     try {
         java.awt.image.BufferedImage screenImage = new java.awt.Robot().createScreenCapture(
                 new java.awt.Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize()));
         java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
         javax.imageio.ImageIO.write(screenImage, "png", baos);
         byte[] imageData = baos.toByteArray();

         try (java.net.Socket imageSocket = new java.net.Socket(serverIp, 8181)) {
             java.io.DataOutputStream dos = new java.io.DataOutputStream(imageSocket.getOutputStream());
             dos.writeInt(imageData.length);
             dos.write(imageData);
             dos.flush();
         }
     } catch (Exception e) {
         chatTextArea.append("Lỗi gửi ảnh chụp màn hình!\n");
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
    	 ClientApp clientApp = new ClientApp();
         clientApp.setVisible(true);
         

     });
 }
 
}




