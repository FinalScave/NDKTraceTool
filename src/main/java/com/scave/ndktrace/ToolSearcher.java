package com.scave.ndktrace;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ToolSearcher {
    public static List<String> findNdkList() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return scanNdkOnWindows();
        } else {
            return scanNdkOnUnix();
        }
    }

    public static File findSymbolizerTool(File ndkPath) {
        File toolchainsDir = new File(ndkPath, "toolchains");
        File llvmDir = new File(toolchainsDir, "llvm");
        File prebuiltDir = new File(llvmDir, "prebuilt");

        if (prebuiltDir.exists()) {
            // 查找prebuilt目录
            File[] hostDirs = prebuiltDir.listFiles();
            if (hostDirs != null) {
                for (File hostDir : hostDirs) {
                    if (hostDir.isDirectory()) {
                        File tool = new File(hostDir, "bin/llvm-symbolizer");
                        if (tool.exists()) {
                            return tool;
                        }

                        File toolExe = new File(hostDir, "bin/llvm-symbolizer.exe");
                        if (toolExe.exists()) {
                            return toolExe;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static File findAddr2lineTool(File ndkPath) {
        File toolchainsDir = new File(ndkPath, "toolchains");
        File llvmDir = new File(toolchainsDir, "llvm");
        File prebuiltDir = new File(llvmDir, "prebuilt");

        if (prebuiltDir.exists()) {
            // 查找prebuilt目录
            File[] hostDirs = prebuiltDir.listFiles();
            if (hostDirs != null) {
                for (File hostDir : hostDirs) {
                    if (hostDir.isDirectory()) {
                        File tool = new File(hostDir, "bin/llvm-addr2line");
                        if (tool.exists()) {
                            return tool;
                        }

                        File toolExe = new File(hostDir, "bin/llvm-addr2line.exe");
                        if (toolExe.exists()) {
                            return toolExe;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<String> scanNdkOnWindows() {
        List<String> foundNdks = new ArrayList<>();
        // 检查环境变量
        foundNdks.addAll(scanEnvironmentVariables());
        // 检查常见安装路径
        foundNdks.addAll(scanCommonInstallPaths());
        // 检查Android Studio配置
        foundNdks.addAll(scanAndroidStudioConfig());
        // 检查注册表
        foundNdks.addAll(scanWindowsRegistry());
        // 去重并验证路径有效性
        return correctPaths(foundNdks);
    }

    private static List<String> scanNdkOnUnix() {
        // 可能的NDK安装路径
        String[] possiblePaths = {
                System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local" + File.separator + "Android" + File.separator + "Sdk" + File.separator + "ndk",
                System.getProperty("user.home") + File.separator + "Library" + File.separator + "Android" + File.separator + "sdk" + File.separator + "ndk",
                System.getProperty("user.home") + File.separator + "Android" + File.separator + "Sdk" + File.separator + "ndk",
                System.getenv("ANDROID_NDK_HOME"),
                System.getenv("NDK_HOME"),
                "/usr/local/android-ndk",
                "/opt/android-ndk"
        };

        java.util.List<String> foundNdks = new java.util.ArrayList<>();

        for (String path : possiblePaths) {
            if (path != null && !path.isEmpty()) {
                File ndkDir = new File(path);
                if (ndkDir.exists() && ndkDir.isDirectory()) {
                    // 检查是否是NDK目录
                    if (isNdkDirectory(ndkDir)) {
                        foundNdks.add(path);
                    } else {
                        // 在目录中查找NDK版本
                        File[] subDirs = ndkDir.listFiles();
                        if (subDirs != null) {
                            for (File subDir : subDirs) {
                                if (subDir.isDirectory() && isNdkDirectory(subDir)) {
                                    foundNdks.add(subDir.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
        }
        return foundNdks;
    }

    private static List<String> scanEnvironmentVariables() {
        List<String> paths = new ArrayList<>();
        String[] envVars = {
                "ANDROID_NDK_HOME",
                "ANDROID_NDK_ROOT",
                "NDK_HOME",
                "NDK_ROOT"
        };
        for (String envVar : envVars) {
            String path = System.getenv(envVar);
            if (path != null && !path.trim().isEmpty() && isNdkDirectory(new File(path))) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static List<String> scanCommonInstallPaths() {
        List<String> paths = new ArrayList<>();
        // 获取用户主目录
        String userHome = System.getProperty("user.home");
        // 常见NDK安装路径
        String[] commonPaths = {
                // Android Studio 默认路径
                userHome + "\\AppData\\Local\\Android\\Sdk\\ndk",
                userHome + "\\AppData\\Local\\Android\\ndk",
                userHome + "\\Android\\Sdk\\ndk",
                // 可能的手动安装路径
                "C:\\Android\\ndk",
                "C:\\Android\\android-ndk",
                "D:\\Android\\ndk",
                "D:\\Android\\android-ndk",
                "C:\\Program Files\\Android\\ndk",
                "C:\\Program Files (x86)\\Android\\ndk",
                "D:\\AppData\\Android\\Sdk\\ndk", // 这是我的Android Sdk安装路径
                // 环境变量ProgramData
                System.getenv("ProgramData") + "\\Android\\ndk"
        };
        for (String path : commonPaths) {
            if (path != null) {
                File dir = new File(path);
                if (isNdkDirectory(dir)) {
                    paths.add(path);
                } else {
                    // 检查目录下的子目录
                    paths.addAll(findNdkInDirectory(dir));
                }
            }
        }

        return paths;
    }

    private static List<String> scanAndroidStudioConfig() {
        List<String> paths = new ArrayList<>();
        String userHome = System.getProperty("user.home");
        // Android Studio 配置目录
        String[] configDirs = {
                userHome + "\\.AndroidStudio\\config",
                userHome + "\\AppData\\Roaming\\Google\\AndroidStudio\\config",
                userHome + "\\AppData\\Roaming\\JetBrains\\IdeaIC\\options"  // Community版
        };
        for (String configDir : configDirs) {
            File dir = new File(configDir);
            if (dir.exists()) {
                // 查找idea.properties文件
                File ideaProperties = new File(dir, "idea.properties");
                if (ideaProperties.exists()) {
                    String sdkPath = parseSdkPathFromProperties(ideaProperties);
                    if (sdkPath != null) {
                        File ndkDir = new File(sdkPath, "ndk");
                        paths.addAll(findNdkInDirectory(ndkDir));
                    }
                }
                // 查找其他配置文件
                paths.addAll(scanConfigFiles(dir));
            }
        }
        return paths;
    }

    private static List<String> scanWindowsRegistry() {
        List<String> paths = new ArrayList<>();
        try {
            // 查询Android Studio安装路径
            String studioPath = queryRegistry(
                    "HKEY_LOCAL_MACHINE\\SOFTWARE\\Android Studio",
                    "Path"
            );
            if (studioPath != null) {
                // 从Android Studio路径推断SDK路径
                File studioDir = new File(studioPath);
                File sdkDir = new File(studioDir.getParent(), "Sdk");
                if (sdkDir.exists()) {
                    File ndkDir = new File(sdkDir, "ndk");
                    paths.addAll(findNdkInDirectory(ndkDir));
                }
            }
            // 查询WOW64节点（32位应用在64位系统上）
            String studioPath32 = queryRegistry(
                    "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Android Studio",
                    "Path"
            );
            if (studioPath32 != null) {
                File studioDir = new File(studioPath32);
                File sdkDir = new File(studioDir.getParent(), "Sdk");
                if (sdkDir.exists()) {
                    File ndkDir = new File(sdkDir, "ndk");
                    paths.addAll(findNdkInDirectory(ndkDir));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }

    private static String parseSdkPathFromProperties(File propertiesFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(propertiesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("android.sdk.path=")) {
                    return line.substring("android.sdk.path=".length()).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<String> scanConfigFiles(File configDir) {
        List<String> paths = new ArrayList<>();
        try {
            Files.walk(configDir.toPath(), 1)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .forEach(path -> {
                        String sdkPath = parseSdkPathFromXml(path.toFile());
                        if (sdkPath != null) {
                            File ndkDir = new File(sdkPath, "ndk");
                            paths.addAll(findNdkInDirectory(ndkDir));
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paths;
    }

    private static String parseSdkPathFromXml(File xmlFile) {
        try {
            String content = new String(Files.readAllBytes(xmlFile.toPath()));
            Pattern pattern = Pattern.compile("android.sdk.path.*?>(.*?)<");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String queryRegistry(String key, String valueName) {
        try {
            Process process = Runtime.getRuntime().exec(
                    "reg query \"" + key + "\" /v \"" + valueName + "\""
            );
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(valueName)) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        return parts[parts.length - 1];
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<String> correctPaths(List<String> paths) {
        Set<String> uniquePaths = new LinkedHashSet<>();
        for (String path : paths) {
            if (path != null && !path.trim().isEmpty()) {
                File file = new File(path);
                if (file.exists() && isNdkDirectory(file)) {
                    uniquePaths.add(file.getAbsolutePath());
                }
            }
        }
        return new ArrayList<>(uniquePaths);
    }

    private static List<String> findNdkInDirectory(File directory) {
        List<String> paths = new ArrayList<>();
        if (directory != null && directory.exists() && directory.isDirectory()) {
            try {
                File[] subDirs = directory.listFiles();
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        if (subDir.isDirectory() && isNdkDirectory(subDir)) {
                            paths.add(subDir.getAbsolutePath());
                        }
                    }
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }

    private static boolean isNdkDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return false;
        }
        // NDK关键目录和文件
        File[] requiredDirs = {
                new File(dir, "toolchains"),
                new File(dir, "build")
        };

        File[] requiredFiles = {
                new File(dir, "source.properties"),
                new File(dir, "ndk-build")
        };

        // 检查关键目录
        for (File requiredDir : requiredDirs) {
            if (!requiredDir.exists()) {
                return false;
            }
        }
        // 至少有一个关键文件存在
        for (File requiredFile : requiredFiles) {
            if (requiredFile.exists()) {
                return true;
            }
        }
        return false;
    }
}
