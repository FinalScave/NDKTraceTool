package com.scave.ndktrace;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

public class NDKTracePanel extends JFrame {
    // 历史记录相关参数
    private static final int MAX_HISTORY_SIZE = 10;
    private static final String PREF_NODE = "com.scave.ndktrace.pathHistory";
    private static final String PREF_COUNT_NDK = "ndk.count";
    private static final String PREF_COUNT_SO = "so.count";
    private static final String PREF_PREFIX_NDK = "ndk.";
    private static final String PREF_PREFIX_SO = "so.";

    // 输入输出控件
    private JComboBox<String> ndkPathField;
    private JComboBox<String> soPathField;
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
        setTitle("NDK堆栈符号还原工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 700));

        // NDK路径输入（支持历史下拉）
        ndkPathField = new JComboBox<>();
        ndkPathField.setEditable(true);
        ndkPathField.setPreferredSize(new Dimension(500, 25));

        // SO路径输入（支持历史下拉）
        soPathField = new JComboBox<>();
        soPathField.setEditable(true);
        soPathField.setPreferredSize(new Dimension(500, 25));

        stackInputArea = new JTextArea(10, 60);
        resultArea = new JTextArea(10, 60);
        resultArea.setEditable(false);

        scanNdkButton = new JButton("扫描NDK");
        browseNdkButton = new JButton("浏览");
        browseSoFileButton = new JButton("选择SO文件");
        browseSoDirButton = new JButton("选择SO目录");
        parseButton = new JButton("解析堆栈");
        clearButton = new JButton("清空");

        // 启动时加载历史（最近一条自动回填）
        loadPathHistory();
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // NDK路径配置面板
        JPanel ndkPanel = createNdkPanel();
        // SO文件配置面板
        JPanel soPanel = createSoPanel();
        // 堆栈输入面板
        JPanel stackPanel = createStackPanel();
        // 按钮面板
        JPanel buttonPanel = createButtonPanel();
        // 结果展示面板
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
        setLocationRelativeTo(null);
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
                BorderFactory.createEtchedBorder(), "堆栈输入",
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
        // 选择NDK路径
        browseNdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseNdkPath();
                addNdkPathToHistory(getNdkPathText());
            }
        });

        // 扫描NDK
        scanNdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> foundNdks = ToolSearcher.findNdkList();
                if (!foundNdks.isEmpty()) {
                    showNdkSelectionDialog(foundNdks);
                } else {
                    JOptionPane.showMessageDialog(NDKTracePanel.this,
                            "未扫描到已安装NDK，请手动选择路径",
                            "扫描结果",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // 选择SO文件
        browseSoFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseSoFile();
                addSoPathToHistory(getSoPathText());
            }
        });

        // 选择SO目录
        browseSoDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseSoDir();
                addSoPathToHistory(getSoPathText());
            }
        });

        // 解析堆栈
        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parseStack();
            }
        });

        // 清空输入输出
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
        chooser.setDialogTitle("选择NDK目录");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            ndkPathField.setSelectedItem(selectedFile.getAbsolutePath());
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
            soPathField.setSelectedItem(selectedFile.getAbsolutePath());
        }
    }

    private void browseSoDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择包含SO文件目录");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            soPathField.setSelectedItem(selectedFile.getAbsolutePath());
        }
    }

    private void showNdkSelectionDialog(List<String> ndkPaths) {
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
                selectNdkFromList(dialog, ndkPaths, list.getSelectedIndex());
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectNdkFromList(dialog, ndkPaths, list.locationToIndex(e.getPoint()));
                }
            }
        });

        dialog.setVisible(true);
    }

    private void selectNdkFromList(JDialog dialog, List<String> ndkPaths, int selectedIndex) {
        if (selectedIndex != -1) {
            ndkPathField.setSelectedItem(ndkPaths.get(selectedIndex));
            addNdkPathToHistory(ndkPaths.get(selectedIndex));
            dialog.dispose();
        }
    }

    private void parseStack() {
        String ndkPath = getNdkPathText();
        String soPath = getSoPathText();
        String stackText = stackInputArea.getText().trim();

        if (ndkPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择NDK路径", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (soPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择SO路径", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (stackText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先输入堆栈文本", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 解析前记录当前路径到历史
        addNdkPathToHistory(ndkPath);
        addSoPathToHistory(soPath);

        try {
            String result = processStackTrace(stackText, new File(soPath), new File(ndkPath));
            resultArea.setText(result);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "解析失败: " + ex.getMessage(),
                    "解析错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String processStackTrace(String stackText, File soPath, File ndkPath) {
        StringBuilder result = new StringBuilder();
        String[] lines = stackText.split("\n");

        for (String line : lines) {
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
            return "找不到llvm-symbolizer或llvm-addr2line工具";
        }

        // 匹配栈地址格式: #00 pc 0005a6c8 /system/lib/libc.so
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

            // 优先用symbolizer，否则回退addr2line
            if (symbolizerTool != null) {
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
                    if (found != null) {
                        return found;
                    }
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

    // ========= 历史记录相关 =========
    private void loadPathHistory() {
        loadComboHistory(ndkPathField, PREF_PREFIX_NDK, PREF_COUNT_NDK);
        loadComboHistory(soPathField, PREF_PREFIX_SO, PREF_COUNT_SO);
    }

    private void loadComboHistory(JComboBox<String> combo, String keyPrefix, String countKey) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        int count = Math.min(prefs.getInt(countKey, 0), MAX_HISTORY_SIZE);

        for (int i = 0; i < count; i++) {
            String value = prefs.get(keyPrefix + i, "").trim();
            if (!value.isEmpty()) {
                model.addElement(value);
            }
        }

        combo.setModel(model);
        if (model.getSize() > 0) {
            // 最近一次放在第一位
            combo.setSelectedIndex(0);
        } else {
            combo.setSelectedItem("");
        }
    }

    private void addNdkPathToHistory(String path) {
        addPathToHistory(ndkPathField, path, PREF_PREFIX_NDK, PREF_COUNT_NDK);
    }

    private void addSoPathToHistory(String path) {
        addPathToHistory(soPathField, path, PREF_PREFIX_SO, PREF_COUNT_SO);
    }

    private void addPathToHistory(JComboBox<String> combo, String rawPath, String keyPrefix, String countKey) {
        if (rawPath == null) {
            return;
        }
        String path = rawPath.trim();
        if (path.isEmpty()) {
            return;
        }

        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) combo.getModel();

        // 去重：已有则先移除
        for (int i = 0; i < model.getSize(); i++) {
            if (path.equals(model.getElementAt(i))) {
                model.removeElementAt(i);
                break;
            }
        }

        // 新路径放到第一位
        model.insertElementAt(path, 0);

        // 控制最大数量
        while (model.getSize() > MAX_HISTORY_SIZE) {
            model.removeElementAt(MAX_HISTORY_SIZE);
        }

        combo.setSelectedItem(path);

        // 持久化到Preferences
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        int size = model.getSize();
        for (int i = 0; i < size; i++) {
            prefs.put(keyPrefix + i, model.getElementAt(i));
        }
        prefs.putInt(countKey, size);
        for (int i = size; i < MAX_HISTORY_SIZE; i++) {
            prefs.remove(keyPrefix + i);
        }
    }

    private String getNdkPathText() {
        return getComboText(ndkPathField);
    }

    private String getSoPathText() {
        return getComboText(soPathField);
    }

    private String getComboText(JComboBox<String> combo) {
        Object item = combo.getEditor().getItem();
        return item == null ? "" : item.toString().trim();
    }
}
