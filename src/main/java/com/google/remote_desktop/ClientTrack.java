package com.google.remote_desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Date;
import java.text.SimpleDateFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;

public class ClientTrack extends JFrame {
    private JTextArea processInfoArea;
    private JTextField serverIpField;
    private JTextField serverPortField;
    private Socket socket;
    //private volatile boolean isListening = true;
    private FileWriter fileWriter;
    private static boolean isLoggingEnabled = false;

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
        btnLog.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				isLoggingEnabled = !isLoggingEnabled;
                if (isLoggingEnabled) {
                	btnLog.setText("Disable Logging");
                } else {
                	btnLog.setText("Enable Logging");
                }
				
			}
		});
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


            if (socket == null || socket.isClosed()) {
                Thread connectionThread = new Thread(() -> connectToServer(serverIp, serverPort));
                connectionThread.start();
            }
            
        });
    }
    
    private void captureScreenAndSend() {
        // Chụp ảnh và gửi đi
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage capture = robot.createScreenCapture(screenRect);

            // Lưu ảnh vào file
            String filePath = "C:\\Users\\ASUS\\Pictures\\screenshot_" + getTimeStamp() + ".png";
            File file = new File(filePath);
            ImageIO.write(capture, "png", file);

            processInfoArea.append("Ảnh đã được lưu tại " + filePath + "\n");

            sendImageToServer(file);
            
            new Thread(() -> {
                closeAndReconnect();
            }).start();

        } catch (AWTException | IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void closeAndReconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            Thread.sleep(500);
            
            String serverIp = serverIpField.getText();
            int serverPort = Integer.parseInt(serverPortField.getText());
            connectToServer(serverIp, serverPort);
        } catch (InterruptedException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendImageToServer(File file) {
        try (OutputStream outputStream = socket.getOutputStream();
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

    private void connectToServer(String serverIp, int serverPort) {
        try {
            socket = new Socket(serverIp, serverPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String message;
            while ((message = in.readLine()) != null) {
            	updateProcessInfo(message);
                
                handleImageMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private String getTimeStamp() {
    	java.util.Date date = new java.util.Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(date);
    }

    private void handleImageMessage(String message) {
        if (message.equals("Capture Image")) {
            captureAndShowImage();
        }
    }

    private void captureAndShowImage() {
        // Xử lý chụp ảnh và hiển thị thông báo
        try {
            BufferedImage image = ImageIO.read(new File("C:\\Users\\ASUS\\Pictures\\path_to_your_captured_image.jpg")); // Đường dẫn ảnh đã chụp
            if (image != null) {
                ImageIcon icon = new ImageIcon(image);
                JOptionPane.showMessageDialog(this, "Image saved at  C:\\Users\\ASUS\\Pictures", "Nontification", JOptionPane.INFORMATION_MESSAGE, icon);
            }
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
