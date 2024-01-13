package com.google.remote_desktop;


import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 *
 * @author ASUS
 */
public class ClientTest extends javax.swing.JFrame {
    

    private Socket socket;
    private Socket imageSocket;
    private DataOutputStream dos;
    private DataOutputStream imageDos;
    private DataInputStream dis;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Creates new form ClientTest
     */
    public ClientTest() {
        setTitle("Client  ");
        initComponents();
        connectButton.addActionListener(e -> executorService.execute(this::connectToServer));
     }
    

//
void connectToServer() {
        String serverIp = serverIpField.getText();
        int serverPort = Integer.parseInt(serverPortField.getText());
        int imagePort = 8181;

        try {
            socket = new Socket(serverIp, serverPort);
            imageSocket = new Socket(serverIp, imagePort);

            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());

            imageDos = new DataOutputStream(imageSocket.getOutputStream());

            chatTextArea.append("Connected to the server.\n");

            executorService.execute(this::processInfoSenderThread);
            executorService.execute(this::captureListenerThread);

        } catch (IOException e) {
            handleConnectionError(e);
        }
    }
//
    private void captureListenerThread() {
        try {
            while (!Thread.interrupted() && socket.isConnected()) {
                String command = dis.readUTF();
                if (command.equals("Capture")) {
                    System.out.println("GetCapture");
                    captureScreenAndSend();
                } else if (command.equals("Shutdown")) {
                    shutDown();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            handleConnectionError(new IOException("Connection reset"));
        }
    }

    private void captureScreenAndSend() {
        try {
            BufferedImage screenImage = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenImage, "png", baos);
            byte[] imageData = baos.toByteArray();

            imageDos.writeInt(imageData.length);
            imageDos.write(imageData);
            imageDos.flush();

        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }
    }
//
    private void processInfoSenderThread() {
        try {
            while (!Thread.interrupted() && socket.isConnected()) {
                String activeProcess = getActiveProcess();
                dos.writeUTF(activeProcess);
                dos.flush();
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            handleConnectionError(new IOException("Connection reset"));
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
            return endStatus;
        } else {
            String beginStatus = timestamp + ": " + "Begin - " + processName;
            return beginStatus;
        }
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }

    private void handleConnectionError(IOException e) {
        chatTextArea.append("Connection to the server failed.\n");
        e.printStackTrace();

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (imageSocket != null && !imageSocket.isClosed()) {
                imageSocket.close();
            }
            if (dos != null) {
                dos.close();
            }
            if (imageDos != null) {
                imageDos.close();
            }
            if (dis != null) {
                dis.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void shutDown() {
        try {
            Runtime.getRuntime().exec("shutdown /s /t 0");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

 
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        mainPanel = new javax.swing.JPanel();
        topPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        serverPortField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        serverIpField = new javax.swing.JTextField();
        connectButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        chatTextArea = new javax.swing.JTextArea();

        jLabel2.setText("jLabel2");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        topPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("Server IP:");
        topPanel.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, 70, 23));

        serverPortField.setText("8080");
        serverPortField.setMinimumSize(new java.awt.Dimension(70, 22));
        serverPortField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverPortFieldActionPerformed(evt);
            }
        });
        topPanel.add(serverPortField, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 10, 150, -1));

        jLabel3.setText("Port");
        topPanel.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 10, 40, 20));

        serverIpField.setText("127.0.0.1");
        serverIpField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverIpFieldActionPerformed(evt);
            }
        });
        topPanel.add(serverIpField, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 10, 140, -1));

        connectButton.setBackground(new java.awt.Color(255, 51, 51));
        connectButton.setText("Connect");
        connectButton.setToolTipText("");
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });
        topPanel.add(connectButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 10, -1, -1));

        chatTextArea.setColumns(20);
        chatTextArea.setRows(5);
        jScrollPane1.setViewportView(chatTextArea);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(topPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 428, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void serverPortFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverPortFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_serverPortFieldActionPerformed

    private void serverIpFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverIpFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_serverIpFieldActionPerformed

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_connectButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ClientTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ClientTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ClientTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ClientTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ClientTest().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea chatTextArea;
    private javax.swing.JButton connectButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTextField serverIpField;
    private javax.swing.JTextField serverPortField;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
}
