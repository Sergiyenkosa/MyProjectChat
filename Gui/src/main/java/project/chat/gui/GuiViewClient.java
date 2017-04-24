package project.chat.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

public class GuiViewClient {
    private final GuiControllerClient controller;

    private JFrame frame = new JFrame("Chat");
    private JTextField textField = new JTextField(50);
    private JTextArea messages = new JTextArea(10, 50);
    private JTextArea users = new JTextArea(10, 10);
    private JList<String> usersSelectList = new JList<>();
    private JButton sendPrivateMessageButton = new JButton("Send private message");
    private JButton sendFileButton = new JButton("Send file message");
    private JButton sendFileForAllButton = new JButton("Send file message for all");

    public GuiViewClient(GuiControllerClient controller) {
        this.controller = controller;
        initView();
    }

    private void initView() {
        textField.setEditable(false);
        sendPrivateMessageButton.setEnabled(false);
        sendFileButton.setEnabled(false);
        sendFileForAllButton.setEnabled(false);
        messages.setEditable(false);
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);
        users.setEditable(false);

        sendPrivateMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog dialog = new JDialog(frame, "Select user", true);

                JButton button = new JButton("ok");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final String receiverName = usersSelectList.getSelectedValue();
                        if (receiverName != null) {
                            final JDialog dialog1 = new JDialog(dialog,"Private message", true);

                            final JTextField privateTextField = new JTextField(50);
                            privateTextField.setMaximumSize(new Dimension(200, 0));
                            privateTextField.setEditable(true);

                            JButton button1 = new JButton("send message");
                            button1.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    String privateMessage = privateTextField.getText();
                                    if (privateMessage.length()>0) {
                                        controller.sendPrivateMessage(privateMessage, receiverName);
                                        dialog1.setVisible(false);
                                        dialog.setVisible(false);
                                    }
                                }
                            });

                            dialog1.add(new JScrollPane(privateTextField), BorderLayout.CENTER);
                            dialog1.add(button1, BorderLayout.SOUTH);
                            dialog1.pack();
                            dialog1.setVisible(true);
                        }
                    }
                });
                dialog.add(new JScrollPane(usersSelectList), BorderLayout.CENTER);
                dialog.add(button, BorderLayout.SOUTH);
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        sendFileForAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new java.io.File("."));
                chooser.setDialogTitle("select file");
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isFile()) {
                    File file = chooser.getSelectedFile();
                    try {
                        controller.sendFileMessageForAll(file.getName(), new FileInputStream(file));
                    } catch (FileNotFoundException e1) {
                        errorMessage("Error file does not exist");
                    }
                }
            }
        });

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog dialog = new JDialog(frame, "Select user", true);

                JButton button = new JButton("ok");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final String receiverName = usersSelectList.getSelectedValue();
                        if (receiverName != null) {

                            JFileChooser chooser = new JFileChooser();
                            chooser.setCurrentDirectory(new java.io.File("."));
                            chooser.setDialogTitle("Select file");
                            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            chooser.setAcceptAllFileFilterUsed(false);

                            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isFile()) {
                                File file = chooser.getSelectedFile();
                                try {
                                    controller.sendFileMessage(receiverName, file.getName(), new FileInputStream(file));
                                    dialog.setVisible(false);
                                } catch (FileNotFoundException e1) {
                                    dialog.setVisible(false);
                                    errorMessage("Error file does not exist");
                                }
                            }
                        }
                    }
                });
                dialog.add(new JScrollPane(usersSelectList), BorderLayout.CENTER);
                dialog.add(button, BorderLayout.SOUTH);
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JScrollPane(users), BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BorderLayout());
        buttonsPanel.add(sendPrivateMessageButton, BorderLayout.NORTH);
        buttonsPanel.add(sendFileButton, BorderLayout.CENTER);
        buttonsPanel.add(sendFileForAllButton, BorderLayout.SOUTH);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        frame.getContentPane().add(textField, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(messages), BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.EAST);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(JOptionPane.showConfirmDialog(frame, controller.askForExit()) == JOptionPane.OK_OPTION){
                    controller.closeAndRemoveAllStreams(false);
                    System.exit(0);
                }
            }
        });
        frame.setVisible(true);

        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                controller.sendTextMessage(textField.getText());
                textField.setText("");
            }
        });
    }

    public String  getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Type the server ip",
                "User configuration",
                JOptionPane.QUESTION_MESSAGE);
    }

    public int getServerPort() {
        while (true) {
            String port = JOptionPane.showInputDialog(
                    frame,
                    "Type the server port",
                    "User configuration",
                    JOptionPane.QUESTION_MESSAGE);
            try {
                return Integer.parseInt(port.trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Error, invalid server port. Please try again.",
                        "Client configuration",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public String getUserName() {
        return JOptionPane.showInputDialog(
                frame,
                "Type the login",
                "Client configuration",
                JOptionPane.QUESTION_MESSAGE);
    }

    public String getUserPassword() {
        return JOptionPane.showInputDialog(
                frame,
                "Type the user password",
                "Client configuration",
                JOptionPane.QUESTION_MESSAGE);
    }

    public void notifyConnectionStatusChanged(boolean clientConnected) {
        textField.setEditable(clientConnected);
        sendPrivateMessageButton.setEnabled(clientConnected);
        sendFileButton.setEnabled(clientConnected);
        sendFileForAllButton.setEnabled(clientConnected);
        if (clientConnected) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Connection is established.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                    frame,
                    "Error connecting to the server",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean askGetFile(String senderName, String fileName) {
        String question = String.format("User %s send file %s for you, download the file?",
                senderName, fileName);

        while (true) {
            int answer = JOptionPane.showConfirmDialog(
                    frame,
                    question,
                    "File message", JOptionPane.YES_NO_OPTION);

            switch (answer) {
                case JOptionPane.YES_OPTION:
                    return true;
                case JOptionPane.NO_OPTION:
                    return false;
            }
        }
    }

    protected File getDirectoryFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Folder select");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        while (true) {
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isDirectory()) {
                return chooser.getSelectedFile();
            }
        }
    }

    public void refreshMessages() {
        messages.setText(controller.getModel().getNewMessage() + "\n" + messages.getText());
    }

    public void refreshUsers() {
        Set<String> usersSet = new HashSet<>(controller.getModel().getAllUserNames());
        StringBuilder sb = new StringBuilder();

        for (String userName : usersSet) {
            sb.append(userName).append("\n");
        }

        users.setText(sb.toString());

        usersSet.remove(controller.getName());

        usersSelectList.setListData(usersSet.toArray(new String[usersSet.size()]));
    }

    public void infoMessage(String message){
        JOptionPane.showMessageDialog(
                frame,
                message,
                "Info",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void errorMessage(String message){
        JOptionPane.showMessageDialog(
                frame,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
