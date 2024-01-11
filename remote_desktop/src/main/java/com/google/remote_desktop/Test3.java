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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.event.ActionListener;



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
    private final Map<String, Long> processUsageMap = new HashMap<>();
    private long currentProcessStartTime;
    
    

    public Test3() {
        setTitle("Client FGDFGDF ");
        setSize(714, 656);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        processInfoArea = new JTextArea();
        processInfoArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(processInfoArea);
        getContentPane().setLayout(new BorderLayout(0, 0));

        serverIpField = new JTextField("127.0.0.1");
        serverPortField = new JTextField("8080");

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Server IP:"));
        topPanel.add(serverIpField);
        topPanel.add(new JLabel("Server Port:"));
        topPanel.add(serverPortField);

        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(scrollPane);

        JPanel sidePanel = new JPanel();
        getContentPane().add(sidePanel, BorderLayout.SOUTH);
        JButton connectButton = new JButton("Connect to Server");
        sidePanel.add(connectButton);
        
        Border border = new LineBorder(Color.BLUE, 1);

        

        connectButton.addActionListener(e -> executorService.execute(this::connectToServer));
        
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(200, 10));
        getContentPane().add(panel, BorderLayout.EAST);
                        panel.setLayout(null);
                
                        JButton btnCapture = new JButton("Capture");
                        btnCapture.setBounds(10, 74, 180, 23);
                        panel.add(btnCapture);
                        btnCapture.addActionListener(e -> executorService.execute(this::sendCaptureCommand));
                
                        JButton btnShut = new JButton("Shut Down");
                        btnShut.setBounds(10, 118, 180, 23);
                        btnShut.setMinimumSize(new Dimension(71, 23));
                        btnShut.setMaximumSize(new Dimension(71, 23));
                        panel.add(btnShut);
        
                JButton btnLog = new JButton("Log");
                btnLog.setBounds(10, 159, 180, 23);
                panel.add(btnLog);
                btnLog.setMinimumSize(new Dimension(71, 23));
                btnLog.setMaximumSize(new Dimension(71, 23));
                btnLog.setPreferredSize(new Dimension(71, 23));
                
                JButton btnNewButton = new JButton("Statistical");
                btnNewButton.addActionListener(new ActionListener() {
                	public void actionPerformed(ActionEvent e) {
                		displayAppUsageStatistics();
                	}
                });
                btnNewButton.setBounds(10, 203, 180, 23);
                panel.add(btnNewButton);
                btnLog.addActionListener(this::toggleLogging);
                
                btnShut.addActionListener(e -> executorService.execute(this::sendShutDownCommand));
    }
    
    private void sendShutDownCommand() {
        try {
            if (dos != null) {
                dos.writeUTF("Shutdown");
                dos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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


                File directory = new File("D:\\image");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File imageFile = new File(directory, "img" + getTime() + ".png");
                ImageIO.write(capturedImage, "png", imageFile);
                System.out.println("Luu hình anh");
                displayImage(capturedImage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void displayImage(BufferedImage capturedImage) {
		int desiredWidth = capturedImage.getWidth() / 3;
		int desiredHeight = capturedImage.getHeight() / 3;
		BufferedImage resizedImage = new BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_ARGB);
		resizedImage.getGraphics().drawImage(capturedImage, 0, 0, desiredWidth, desiredHeight, null);
		ImageIcon icon = new ImageIcon(resizedImage);
		JOptionPane.showMessageDialog(
		        null,
		        icon,
		        "Hình ảnh",
		        JOptionPane.PLAIN_MESSAGE
		);
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
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
    
    private void displayAppUsageStatistics() {
        System.out.println("DEBUG: processUsageMap = " + processUsageMap);
        StringBuilder message = new StringBuilder("Thống Kê Thời Gian Chạy Tiến Trình:\n");
        for (Map.Entry<String, Long> entry : processUsageMap.entrySet()) {
            String processName = entry.getKey();
            long processUsage = entry.getValue();

            if (processUsage > 0) {
                message.append("[").append(processName).append("] : ").append(formatTime(processUsage)).append("\n");
            }
        }
        if (message.length() == 0) {
            message.append("Không có dữ liệu thống kê.");
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                JOptionPane.showMessageDialog(Test3.this, message.toString(), "Thống Kê Thời Gian Chạy", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        };

        worker.execute();
    }
    private void updateProcessInfo(String info) {
        processInfoArea.append(info + "\n");
        if (isLoggingEnabled) {
            logProcessInfo(info);
        }
        String[] parts = info.split(" - ");
        if (parts.length == 3) {
            String status = parts[1].trim();
            String processName = parts[2].trim();

            if (status.equals("Begin")) {
                currentProcessStartTime = System.currentTimeMillis();
            } else if (status.equals("End") || status.equals("Running")) {
                long processUsage = System.currentTimeMillis() - currentProcessStartTime;
                processUsageMap.merge(processName, processUsage, Long::sum);

                if (status.equals("End")) {
                    currentProcessStartTime = 0;
                }
            }
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