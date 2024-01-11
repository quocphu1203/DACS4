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

public class test1 extends JFrame {
    private JTextField serverIpField;
    private JTextField serverPortField;
    private JButton connectButton;
    private JTextArea chatTextArea;
    private Socket socket;
    private Socket imageSocket;
    private DataOutputStream dos;
    private DataOutputStream imageDos;
    private DataInputStream dis;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public test1() {
        setTitle("Client Application FSDFSDF");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        serverIpField = new JTextField("127.0.0.1");
        serverPortField = new JTextField("8080");
        connectButton = new JButton("Connect to Server");
        chatTextArea = new JTextArea(10, 30);

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

        connectButton.addActionListener(e -> executorService.execute(this::connectToServer));
    }

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
            String endStatus = timestamp + " - " + "End - " + lastProcessName;
            lastProcessName = processName;
            String beginStatus = timestamp + " - " + "Begin - " + processName;
            return endStatus + "\n" + beginStatus;
        }

        return timestamp + " - " + "Running - " + processName;
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date());
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
            chatTextArea.append("Server is closed. \n");
        }
    }
    

    private void shutDown() {
        try {
            Runtime.getRuntime().exec("shutdown /s /t 0");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            test1 clientApp = new test1();
            clientApp.setVisible(true);
        });
    }
}
