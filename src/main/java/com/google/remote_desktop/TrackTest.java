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
    private final Map<String, Long> processUsageMap = new HashMap<>();
    private long currentProcessStartTime;

    public TrackTest() {
    	setTitle("Client to Receive Process Info");
        initComponents();
        
        connectButton.addActionListener(e -> executorService.execute(this::connectToServer)); 
        btnLog.addActionListener(this::toggleLogging);
        btnShut.addActionListener(e -> executorService.execute(this::sendShutDownCommand));
        btnCapture.addActionListener(e -> executorService.execute(this::sendCaptureCommand));
        btnStatis.addActionListener(e -> executorService.execute(this::displayAppUsageStatistics));
        
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


                File directory = new File("C:/Users/ACER/OneDrive - MSFT/Pictures/Phim");
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
                JOptionPane.showMessageDialog(TrackTest.this, message.toString(), "Thống Kê Thời Gian Chạy", JOptionPane.INFORMATION_MESSAGE);
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
        jScrollPane1 = new javax.swing.JScrollPane();
        processInfoArea = new javax.swing.JTextArea();
        sidePanel = new javax.swing.JPanel();
        btnStatis = new javax.swing.JButton();
        btnLog = new javax.swing.JButton();
        btnCapture = new javax.swing.JButton();

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
        connectButton.setIcon(new ImageIcon("C:\\Users\\ACER\\IdeaProjects\\DACS\\DACS4\\src\\main\\java\\com\\google\\remote_desktop\\icon\\link.png")); // NOI18N
        connectButton.setText("Connect");
        topPanel.add(connectButton);

        getContentPane().add(topPanel, java.awt.BorderLayout.NORTH);

        botPanel.setBackground(new java.awt.Color(255, 255, 255));
        botPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        btnShut.setBackground(new java.awt.Color(102, 153, 255));
        btnShut.setForeground(new java.awt.Color(153, 0, 0));
        btnShut.setIcon(new ImageIcon("C:\\Users\\ACER\\IdeaProjects\\DACS\\DACS4\\src\\main\\java\\com\\google\\remote_desktop\\icon\\on-off-button.png")); // NOI18N
        btnShut.setText("Shut down");

        javax.swing.GroupLayout botPanelLayout = new javax.swing.GroupLayout(botPanel);
        botPanel.setLayout(botPanelLayout);
        botPanelLayout.setHorizontalGroup(
            botPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(botPanelLayout.createSequentialGroup()
                .addGap(243, 243, 243)
                .addComponent(btnShut, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(245, Short.MAX_VALUE))
        );
        botPanelLayout.setVerticalGroup(
            botPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(botPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnShut)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(botPanel, java.awt.BorderLayout.SOUTH);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(360, 86));

        processInfoArea.setBackground(new java.awt.Color(51, 51, 51));
        processInfoArea.setColumns(20);
        processInfoArea.setForeground(new java.awt.Color(0, 0, 0));
        processInfoArea.setRows(5);
        processInfoArea.setEnabled(false);
        processInfoArea.setPreferredSize(new java.awt.Dimension(232, 600));
        processInfoArea.setSelectedTextColor(new java.awt.Color(0, 0, 0));
        processInfoArea.setSelectionColor(new java.awt.Color(0, 0, 0));
        jScrollPane1.setViewportView(processInfoArea);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 674, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        getContentPane().add(mainPanel, java.awt.BorderLayout.WEST);

        sidePanel.setBackground(new java.awt.Color(25, 29, 74));

        btnStatis.setBackground(new java.awt.Color(255, 255, 255));
        btnStatis.setIcon(new ImageIcon("C:\\Users\\ACER\\IdeaProjects\\DACS\\DACS4\\src\\main\\java\\com\\google\\remote_desktop\\icon\\statistics.png")); // NOI18N
        btnStatis.setText("Statistics");
        btnStatis.setIconTextGap(8);
        btnStatis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStatisActionPerformed(evt);
            }
        });

        btnLog.setBackground(new java.awt.Color(255, 255, 255));
        btnLog.setIcon(new ImageIcon("C:\\Users\\ACER\\IdeaProjects\\DACS\\DACS4\\src\\main\\java\\com\\google\\remote_desktop\\icon\\files.png")); // NOI18N
        btnLog.setText("Log");
        btnLog.setIconTextGap(12);
        btnLog.setInheritsPopupMenu(true);

        btnCapture.setBackground(new java.awt.Color(255, 255, 255));
        btnCapture.setIcon(new ImageIcon("C:\\Users\\ACER\\IdeaProjects\\DACS\\DACS4\\src\\main\\java\\com\\google\\remote_desktop\\icon\\photo-capture.png")); // NOI18N
        btnCapture.setText("Capture");
        btnCapture.setIconTextGap(8);

        javax.swing.GroupLayout sidePanelLayout = new javax.swing.GroupLayout(sidePanel);
        sidePanel.setLayout(sidePanelLayout);
        sidePanelLayout.setHorizontalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(btnStatis, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                    .addComponent(btnLog, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCapture, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(11, Short.MAX_VALUE))
        );
        sidePanelLayout.setVerticalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(btnLog)
                .addGap(31, 31, 31)
                .addComponent(btnStatis)
                .addGap(35, 35, 35)
                .addComponent(btnCapture)
                .addContainerGap())
        );

        getContentPane().add(sidePanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStatisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStatisActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnStatisActionPerformed


    public static void main(String args[]) {
       
        /* Create and display the form */
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
    private javax.swing.JButton connectButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTextArea processInfoArea;
    private javax.swing.JTextField serverIpField;
    private javax.swing.JTextField serverPortField;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
}