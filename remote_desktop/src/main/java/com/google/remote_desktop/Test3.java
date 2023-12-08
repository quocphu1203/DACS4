package com.google.remote_desktop;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test3 extends JFrame {
    private JTextArea processInfoArea;
    private JTextField serverIpField;
    private JTextField serverPortField;
    private Socket socket;
    private Socket imageSocket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private DataInputStream imageDis;
    private volatile boolean isLoggingEnabled = false;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private FileWriter fileWriter;

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
        btnLog.addActionListener(this::toggleLogging);
        sidePanel.add(btnLog);

        JButton btnCapture = new JButton("Capture");
        btnCapture.addActionListener(e -> executorService.execute(this::sendCaptureCommand));
        sidePanel.add(btnCapture);

        JButton btnControl = new JButton("Control");
        sidePanel.add(btnControl);

        connectButton.addActionListener(e -> executorService.execute(this::connectToServer));
    }

    private void toggleLogging(ActionEvent e) {
        isLoggingEnabled = !isLoggingEnabled;
        SwingUtilities.invokeLater(() -> ((JButton) e.getSource()).setText(isLoggingEnabled ? "Disable Logging" : "Enable Logging"));
    }

    private void connectToServer() {
        String serverIp = serverIpField.getText();
        int serverPort = Integer.parseInt(serverPortField.getText());
        int serverImagePort = 8181;

        try {
            socket = new Socket(serverIp, serverPort);
            imageSocket = new Socket(serverIp, serverImagePort);

            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            imageDis = new DataInputStream(imageSocket.getInputStream());

            executorService.execute(this::captureScreenThread);
            executorService.execute(this::startProcessInfoThread);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendCaptureCommand() {
        try {
            if (dos != null) {
                dos.writeUTF("Capture");
                dos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void captureScreenThread() {
        try {
            while (!imageSocket.isClosed()) {
                int imageSize = imageDis.readInt();
                byte[] imageData = new byte[imageSize];

                imageDis.readFully(imageData);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData);
                BufferedImage capturedImage = ImageIO.read(byteArrayInputStream);


                File directory = new File("D:/image/");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File imageFile = new File(directory, "img" + getTime() + ".png");
                ImageIO.write(capturedImage, "png", imageFile);
                System.out.println("Luu hình anh");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String getTime() {
        return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
    }

    private void startProcessInfoThread() {
        try {
            while (!socket.isClosed()) {
                String message = dis.readUTF();
                SwingUtilities.invokeLater(() -> updateProcessInfo(message));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateProcessInfo(String info) {
        processInfoArea.append(info + "\n");
        if (isLoggingEnabled) {
            logProcessInfo(info);
        }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        	Test3 clientToReceive = new Test3();
            clientToReceive.setVisible(true);
        });
    }
}