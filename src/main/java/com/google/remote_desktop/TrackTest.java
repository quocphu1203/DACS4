package com.google.remote_desktop;

import javax.imageio.ImageIO;
import javax.swing.*;
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




public class TrackTest extends javax.swing.JFrame {

    private Socket socket;
    private Socket imageSocket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private DataInputStream imageDis;
    private volatile boolean isLoggingEnabled = false;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private FileWriter fileWriter;
    private final Map<String, AppStatistics> appStatisticsMap = new HashMap<>();
    private final Map<String, Long> processUsageMap = new HashMap<>();
    private long currentProcessStartTime;

    private volatile boolean isProcessInfoReceiving = true;

    public TrackTest() {
        setTitle("Client to Receive ");
        initComponents();

        connectButton.addActionListener(e -> executorService.execute(this::connectToServer));
        btnLog.addActionListener(this::toggleLogging);
        btnShut.addActionListener(e -> executorService.execute(this::sendShutDownCommand));
        btnCapture.addActionListener(e -> executorService.execute(this::sendCaptureCommand));

        btnStop.addActionListener(e -> {
            isProcessInfoReceiving = !isProcessInfoReceiving;
            String buttonText = isProcessInfoReceiving ? "Stop" : "Resume";
            btnStop.setText(buttonText);
        });

        btnStatis.addActionListener(this::btnStatisActionPerformed);
    }

    private void btnStatisActionPerformed(java.awt.event.ActionEvent evt) {                                          
        displayStatistics();
    }
    
public class AppStatistics {

    private int usageCount;
    private long totalUsageTime;

    public AppStatistics() {
        this.usageCount = 0;
        this.totalUsageTime = 0;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public long getTotalUsageTime() {
        return totalUsageTime;
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void addToTotalUsageTime(long time) {
        this.totalUsageTime += time;
    }
    
    public AppStatistics(String appName) {
        this(); 
    }
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
        SwingUtilities.invokeLater(() -> btnLog.setText(isLoggingEnabled ? "Disable Logging" : "Enable Logging"));
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

    private static void displayImage(BufferedImage capturedImage) {
        int desiredWidth = capturedImage.getWidth() / 3;
        int desiredHeight = capturedImage.getHeight() / 3;

        BufferedImage resizedImage = new BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_ARGB);

        resizedImage.getGraphics().drawImage(capturedImage, 0, 0, desiredWidth, desiredHeight, null);
        ImageIcon icon = new ImageIcon(resizedImage);

        JOptionPane.showMessageDialog(
                null,
                icon,
                "Image",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private static String getTime() {
        return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
    }

    private void startProcessInfoThread() {
        try {
            while (!socket.isClosed()) {
                if (isProcessInfoReceiving) {
                    String message = dis.readUTF();
                    SwingUtilities.invokeLater(() -> updateProcessInfo(message));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractAppName(String processInfo) {
        String[] parts = processInfo.split(" - ");
        return parts[parts.length - 1].trim();
    }

     private void updateProcessInfo(String info) {
        String appName = extractAppName(info);
        processInfoArea.append(appName + "\n");

        if (isLoggingEnabled) {
            logProcessInfo(info);
        }

        processInfoArea.append(info + "\n");

        // Thống kê dựa trên appName
        appStatisticsMap.computeIfAbsent(appName, k -> new AppStatistics()).incrementUsageCount();

//        appStatisticsMap.computeIfAbsent(appName, AppStatistics::new).incrementUsageCount();
        if (currentProcessStartTime > 0) {
            long processUsage = System.currentTimeMillis() - currentProcessStartTime;
            appStatisticsMap.get(appName).addToTotalUsageTime(processUsage);
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
     
         private void displayStatistics() {
        StringBuilder statistics = new StringBuilder();
        statistics.append("Tổng số lượng ứng dụng đã sử dụng: ").append(appStatisticsMap.size()).append("\n\n");
        statistics.append("Tên các ứng dụng đã sử dụng:\n");

        for (Map.Entry<String, AppStatistics> entry : appStatisticsMap.entrySet()) {
            String appName = entry.getKey();
            AppStatistics appStats = entry.getValue();

            statistics.append("- ").append(appName).append("\n");
            statistics.append("  + Số lượng sử dụng: ").append(appStats.getUsageCount()).append("\n");
            statistics.append("  + Tổng thời gian sử dụng: ").append(appStats.getTotalUsageTime()).append(" milliseconds\n\n");
        }

        // Hiển thị kết quả trên usageScreen
        usageScreen.setText(statistics.toString());
    }

    private void logProcessInfo(String info) {
        try {
            if (fileWriter == null) {
                createNewLogFile();
            }

            fileWriter.write(info + "\n");
            fileWriter.flush();

            checkAndHandleLogFileCount();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNewLogFile() throws IOException {
        String logFileName = "Log_" + DateTimeFormatter.ofPattern("dd-MM-yyyy").format(LocalDateTime.now()) + ".txt";
        fileWriter = new FileWriter(logFileName, true);
    }

    private void checkAndHandleLogFileCount() {
        File logDirectory = new File(".");
        File[] logFiles = logDirectory.listFiles((dir, name) -> name.startsWith("Log_") && name.endsWith(".txt"));

        if (logFiles != null && logFiles.length >= 7) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure to do the 8th overwrite? When done, previous attempts will be lost..",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                for (File logFile : logFiles) {
                    logFile.delete();
                }

                try {
                    createNewLogFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION) {
                JOptionPane.getRootFrame().dispose();
            }
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        topPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        serverIpField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        serverPortField = new javax.swing.JTextField();
        connectButton = new javax.swing.JButton();
        botPanel = new javax.swing.JPanel();
        btnShut = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        processInfoArea = new javax.swing.JTextArea();
        sidePanel = new javax.swing.JPanel();
        btnLog = new javax.swing.JButton();
        btnCapture = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        btnStatis = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        usageScreen = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        topPanel.setBackground(new java.awt.Color(15, 19, 52));
        topPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        topPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Server IP:");
        jLabel1.setMaximumSize(new java.awt.Dimension(48, 25));
        jLabel1.setMinimumSize(new java.awt.Dimension(48, 25));
        jLabel1.setPreferredSize(new java.awt.Dimension(80, 20));
        topPanel.add(jLabel1);

        serverIpField.setText("127.0.0.1");
        topPanel.add(serverIpField);

        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Port:");
        jLabel2.setPreferredSize(new java.awt.Dimension(40, 20));
        topPanel.add(jLabel2);

        serverPortField.setText("8080");
        topPanel.add(serverPortField);

        connectButton.setBackground(new java.awt.Color(255, 255, 255));
        connectButton.setForeground(new java.awt.Color(51, 153, 0));
        connectButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/link.png"))); // NOI18N
        connectButton.setText("Connect");
        topPanel.add(connectButton);

        getContentPane().add(topPanel, java.awt.BorderLayout.NORTH);

        botPanel.setBackground(new java.awt.Color(255, 255, 255));
        botPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        btnShut.setBackground(new java.awt.Color(102, 153, 255));
        btnShut.setForeground(new java.awt.Color(153, 0, 0));
        btnShut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/on-off-button.png"))); // NOI18N
        btnShut.setText("Shut down");

        javax.swing.GroupLayout botPanelLayout = new javax.swing.GroupLayout(botPanel);
        botPanel.setLayout(botPanelLayout);
        botPanelLayout.setHorizontalGroup(
            botPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(botPanelLayout.createSequentialGroup()
                .addGap(342, 342, 342)
                .addComponent(btnShut, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(370, Short.MAX_VALUE))
        );
        botPanelLayout.setVerticalGroup(
            botPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(botPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnShut)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(botPanel, java.awt.BorderLayout.SOUTH);

        mainPanel.setBackground(new java.awt.Color(0, 0, 0));

        processInfoArea.setBackground(new java.awt.Color(0, 0, 0));
        processInfoArea.setColumns(20);
        processInfoArea.setForeground(new java.awt.Color(51, 153, 255));
        processInfoArea.setRows(5);
        jScrollPane2.setViewportView(processInfoArea);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 690, Short.MAX_VALUE)
        );

        getContentPane().add(mainPanel, java.awt.BorderLayout.WEST);

        sidePanel.setBackground(new java.awt.Color(25, 29, 74));

        btnLog.setBackground(new java.awt.Color(255, 255, 255));
        btnLog.setIcon(new javax.swing.ImageIcon("C:\\Users\\ASUS\\git\\repository\\remote_desktop\\src\\main\\java\\com\\google\\remote_desktop\\icon\\files.png")); // NOI18N
        btnLog.setText("Log");
        btnLog.setIconTextGap(12);
        btnLog.setInheritsPopupMenu(true);

        btnCapture.setBackground(new java.awt.Color(255, 255, 255));
        btnCapture.setIcon(new javax.swing.ImageIcon(getClass().getResource("/photo-capture.png"))); // NOI18N
        btnCapture.setText("Capture");
        btnCapture.setIconTextGap(8);

        btnStop.setBackground(new java.awt.Color(255, 255, 255));
        btnStop.setText("Stop");

        btnStatis.setBackground(new java.awt.Color(255, 255, 255));
        btnStatis.setIcon(new javax.swing.ImageIcon(getClass().getResource("/statistics.png"))); // NOI18N
        btnStatis.setText("Statistic");

        usageScreen.setColumns(20);
        usageScreen.setRows(5);
        jScrollPane1.setViewportView(usageScreen);

        javax.swing.GroupLayout sidePanelLayout = new javax.swing.GroupLayout(sidePanel);
        sidePanel.setLayout(sidePanelLayout);
        sidePanelLayout.setHorizontalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(btnLog, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCapture, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                    .addComponent(btnStop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnStatis, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        sidePanelLayout.setVerticalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnLog)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnStatis)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCapture)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnStop, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 529, Short.MAX_VALUE)
                .addContainerGap())
        );

        getContentPane().add(sidePanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents


    public static void main(String args[]) {
       
 java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TrackTest().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel botPanel;
    private javax.swing.JButton btnCapture;
    private javax.swing.JButton btnLog;
    private javax.swing.JButton btnShut;
    private javax.swing.JButton btnStatis;
    private javax.swing.JButton btnStop;
    private javax.swing.JButton connectButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTextArea processInfoArea;
    private javax.swing.JTextField serverIpField;
    private javax.swing.JTextField serverPortField;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JPanel topPanel;
    private javax.swing.JTextArea usageScreen;
    // End of variables declaration//GEN-END:variables
}