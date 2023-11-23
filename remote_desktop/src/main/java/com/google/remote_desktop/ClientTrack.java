package com.google.remote_desktop;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;



public class ClientTrack extends JFrame {
    private JTextArea processInfoArea;
    private JTextField serverIpField;
    private JTextField serverPortField;

    public ClientTrack() {
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
        btnCapture.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                captureScreenAndSend();
            }
        });
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
    
    private void captureScreenAndSend() {
        // Chụp ảnh màn hình từ Test1
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage capture = robot.createScreenCapture(screenRect);

            // Lưu ảnh vào file
            String filePath = "C:\\Users\\ASUS\\Pictures\\screenshot_" + getTimeStamp() + ".png";
            File file = new File(filePath);
            ImageIO.write(capture, "png", file);

            // Hiển thị thông báo tại Test3
            processInfoArea.append("Ảnh đã được lưu tại " + filePath + "\n");

            // Gửi ảnh từ Test3 tới Test2
            sendImageToServer(file);

        } catch (AWTException | IOException ex) {
            ex.printStackTrace();
        }
    }
    
    

    
    private void sendImageToServer(File file) {
        // Kết nối tới Test2 để gửi ảnh
        String serverIp = serverIpField.getText();
        int serverPort = Integer.parseInt(serverPortField.getText());

        try (Socket socket = new Socket(serverIp, serverPort);
             OutputStream outputStream = socket.getOutputStream();
             FileInputStream fileInputStream = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String getTimeStamp() {
    	java.util.Date date = new java.util.Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(date);
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
        	ClientTrack clientToReceive = new ClientTrack();
            clientToReceive.setVisible(true);
        });
    }
}
