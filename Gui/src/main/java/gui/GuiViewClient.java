package gui;

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

    private JFrame frame = new JFrame("Чат");
    private JTextField textField = new JTextField(50);
    private JTextArea messages = new JTextArea(10, 50);
    private JTextArea users = new JTextArea(10, 10);
    private JList<String> usersSelectList = new JList<>();
    private JButton sendPrivateMessageButton = new JButton("Отправить приватное сообщение");
    private JButton sendFileButton = new JButton("Отправить файл");
    private JButton sendFileForAllButton = new JButton("Отправить файл для всех");

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
                final JDialog dialog = new JDialog(frame, "Выбор пользователя", true);

                JButton button = new JButton("Выбрать");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final String receiverName = usersSelectList.getSelectedValue();
                        if (receiverName != null) {
                            final JDialog dialog1 = new JDialog(dialog,"Текст сообщения", true);

                            final JTextField privateTextField = new JTextField(50);
                            privateTextField.setMaximumSize(new Dimension(200, 0));
                            privateTextField.setEditable(true);

                            JButton button1 = new JButton("Отправить");
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
                chooser.setDialogTitle("Выбор файла");
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isFile()) {
                    File file = chooser.getSelectedFile();
                    try {
                        controller.sendFileMessageForAll(file.getName(), new FileInputStream(file));
                    } catch (FileNotFoundException e1) {
                        errorMessage("Такого файла не существует");
                    }
                }
            }
        });

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog dialog = new JDialog(frame, "Выбор пользователя", true);

                JButton button = new JButton("Выбрать");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final String receiverName = usersSelectList.getSelectedValue();
                        if (receiverName != null) {

                            JFileChooser chooser = new JFileChooser();
                            chooser.setCurrentDirectory(new java.io.File("."));
                            chooser.setDialogTitle("Выбор файла");
                            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            chooser.setAcceptAllFileFilterUsed(false);

                            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isFile()) {
                                File file = chooser.getSelectedFile();
                                try {
                                    controller.sendFileMessage(receiverName, file.getName(), new FileInputStream(file));
                                    dialog.setVisible(false);
                                } catch (FileNotFoundException e1) {
                                    dialog.setVisible(false);
                                    errorMessage("Такого файла не существует");
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
                "Введите адрес сервера:",
                "Конфигурация клиента",
                JOptionPane.QUESTION_MESSAGE);
    }

    public int getServerPort() {
        while (true) {
            String port = JOptionPane.showInputDialog(
                    frame,
                    "Введите порт сервера:",
                    "Конфигурация клиента",
                    JOptionPane.QUESTION_MESSAGE);
            try {
                return Integer.parseInt(port.trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Был введен некорректный порт сервера. Попробуйте еще раз.",
                        "Конфигурация клиента",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public String getUserName() {
        return JOptionPane.showInputDialog(
                frame,
                "Введите ваше имя:",
                "Конфигурация клиента",
                JOptionPane.QUESTION_MESSAGE);
    }

    public String getUserPassword() {
        return JOptionPane.showInputDialog(
                frame,
                "Введите ваш пароль:",
                "Конфигурация клиента",
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
                    "Соединение с сервером установлено",
                    "Чат",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                    frame,
                    "Клиент не подключен к серверу",
                    "Чат",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean askGetFile(String senderName, String fileName) {
        String question = String.format("Пользователь:\"%s\",\nотправил вам файл:\n\"%s\".\nПринять файл?",
                senderName, fileName);

        while (true) {
            int answer = JOptionPane.showConfirmDialog(
                    frame,
                    question,
                    "Фаил", JOptionPane.YES_NO_OPTION);

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
        chooser.setDialogTitle("Выбор каталога");
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
                "Чат",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void errorMessage(String message){
        JOptionPane.showMessageDialog(
                frame,
                message,
                "Чат",
                JOptionPane.ERROR_MESSAGE);
    }
}
