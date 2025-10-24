package com.scave.ndktrace;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

public class NDKTracePanel extends JFrame {
    private JTextField ndkPathField;
    private JTextField soPathField;
    private JTextArea stackInputArea;
    private JTextArea resultArea;
    private JButton scanNdkButton;
    private JButton browseNdkButton;
    private JButton browseSoFileButton;
    private JButton browseSoDirButton;
    private JButton parseButton;
    private JButton clearButton;

    public NDKTracePanel() {
        initComponents();
        setupLayout();
        setupListeners();
    }

    private void initComponents() {
        setTitle("NDK堆栈还原工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 700));

        ndkPathField = new JTextField(30);
        soPathField = new JTextField(30);
        stackInputArea = new JTextArea(10, 60);
        resultArea = new JTextArea(10, 60);
        resultArea.setEditable(false);

        scanNdkButton = new JButton("扫描NDK");
        browseNdkButton = new JButton("浏览");
        browseSoFileButton = new JButton("选择SO文件");
        browseSoDirButton = new JButton("选择SO目录");
        parseButton = new JButton("解析堆栈");
        clearButton = new JButton("清空");
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // NDK路径配置区域
        JPanel ndkPanel = createNdkPanel();
        // SO文件配置区域
        JPanel soPanel = createSoPanel();
        // 堆栈输入区域
        JPanel stackPanel = createStackPanel();
        // 按钮区域
        JPanel buttonPanel = createButtonPanel();
        // 结果显示区域
        JPanel resultPanel = createResultPanel();

        mainPanel.add(ndkPanel, BorderLayout.NORTH);
        mainPanel.add(soPanel, BorderLayout.CENTER);
        mainPanel.add(stackPanel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(buttonPanel, BorderLayout.NORTH);
        centerPanel.add(resultPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.NORTH);
        add(new JScrollPane(centerPanel), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null); // 居中显示
    }

    private JPanel createNdkPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "NDK路径配置",
                TitledBorder.LEFT, TitledBorder.TOP));

        panel.add(new JLabel("NDK路径:"));
        panel.add(ndkPathField);
        panel.add(browseNdkButton);
        panel.add(scanNdkButton);

        return panel;
    }

    private JPanel createSoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "SO文件配置",
                TitledBorder.LEFT, TitledBorder.TOP));

        panel.add(new JLabel("SO文件路径:"));
        panel.add(soPathField);
        panel.add(browseSoFileButton);
        panel.add(browseSoDirButton);

        return panel;
    }

    private JPanel createStackPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "崩溃堆栈输入",
                TitledBorder.LEFT, TitledBorder.TOP));

        JScrollPane scrollPane = new JScrollPane(stackInputArea);
        scrollPane.setPreferredSize(new Dimension(800, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(parseButton);
        panel.add(clearButton);
        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "解析结果",
                TitledBorder.LEFT, TitledBorder.TOP));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(800, 250));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void setupListeners() {
        browseNdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseNdkPath();
            }
        });

        scanNdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> foundNdks = ToolSearcher.findNdkList();
                if (!foundNdks.isEmpty()) {
                    showNdkSelectionDialog(foundNdks);
                } else {
                    JOptionPane.showMessageDialog(NDKTracePanel.this,
                            "未找到已安装的NDK，请手动选择路径",
                            "扫描结果",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        browseSoFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseSoFile();
            }
        });

        browseSoDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseSoDir();
            }
        });

        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parseStack();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });
    }

    private void browseNdkPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择NDK根目录");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            ndkPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void browseSoFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("选择SO文件");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SO文件", "so"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            soPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void browseSoDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择包含SO文件的目录");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            soPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void showNdkSelectionDialog(java.util.List<String> ndkPaths) {
        JDialog dialog = new JDialog(this, "选择NDK版本", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);

        JLabel label = new JLabel("找到以下NDK版本，请选择:");
        JList<String> list = new JList<>(ndkPaths.toArray(new String[0]));
        JScrollPane scrollPane = new JScrollPane(list);

        JButton selectButton = new JButton("选择");
        JButton cancelButton = new JButton("取消");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);

        dialog.add(label, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex != -1) {
                    ndkPathField.setText(ndkPaths.get(selectedIndex));
                    dialog.dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    private void parseStack() {
        String ndkPath = ndkPathField.getText().trim();
        String soPath = soPathField.getText().trim();
        String stackText = stackInputArea.getText().trim();

        if (ndkPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择NDK路径", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (soPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择SO文件路径", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (stackText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入崩溃堆栈信息", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String result = processStackTrace(stackText, new File(soPath), new File(ndkPath));
            resultArea.setText(result);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "解析过程中发生错误: " + ex.getMessage(),
                    "解析错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String processStackTrace(String stackText, File soPath, File ndkPath) {
        StringBuilder result = new StringBuilder();
        String[] lines = stackText.split("\n");

        for (String line : lines) {
            //result.append(line).append("\n");
            String parsedInfo = parseStackLine(line, soPath, ndkPath);
            if (parsedInfo != null) {
                result.append(parsedInfo).append("\n\n");
            }
        }

        return result.toString();
    }

    private String parseStackLine(String line, File soPath, File ndkPath) {
        File symbolizerTool = ToolSearcher.findSymbolizerTool(ndkPath);
        File addr2lineTool = ToolSearcher.findAddr2lineTool(ndkPath);
        if (symbolizerTool == null && addr2lineTool == null) {
            return "未找到llvm-symbolizer或llvm-add2line工具";
        }
        // 匹配堆栈地址格式，例如: #00 pc 0005a6c8  /system/lib/libc.so
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*pc\\s+([0-9a-fA-F]+)\\s+([^\\s]+)");
        java.util.regex.Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String address = matcher.group(1);
            String libPath = matcher.group(2);
            String libName = new File(libPath).getName();

            File soFile = findSoFile(libName, soPath);
            if (soFile == null || !soFile.exists()) {
                return null;
            }
            if (symbolizerTool != null) {
                // llvm-symbolizer只识别7位地址(0x后面7位)
                if (!address.startsWith("0x")) {
                    if (address.length() > 7) {
                        address = address.substring(address.length() - 7);
                    }
                    address = "0x" + address;
                }
                return "#" + address + " => " + ToolExecutor.execSymbolizer(soFile, address, symbolizerTool);
            } else {
                return "#" + address + " => " + ToolExecutor.execAddr2line(soFile, address, addr2lineTool);
            }
        }

        return null;
    }

    private File findSoFile(String libName, File soPath) {
        if (soPath.isFile() && soPath.getName().equals(libName)) {
            return soPath;
        } else if (soPath.isDirectory()) {
            return findFileInDirectory(soPath, libName);
        }
        return null;
    }

    private File findFileInDirectory(File directory, String fileName) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File found = findFileInDirectory(file, fileName);
                    if (found != null) return found;
                } else if (file.getName().equals(fileName)) {
                    return file;
                }
            }
        }
        return null;
    }

    private void clearAll() {
        stackInputArea.setText("");
        resultArea.setText("");
    }
}