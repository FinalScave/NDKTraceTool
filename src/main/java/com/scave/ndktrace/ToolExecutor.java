package com.scave.ndktrace;

import java.io.File;

public final class ToolExecutor {
    public static String execSymbolizer(File soFile, String address, File toolFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    toolFile.getAbsolutePath(),
                    "-e", soFile.getAbsolutePath(),
                    address
            );

            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                return "解析失败，退出码: " + exitCode;
            }
        } catch (Exception e) {
            return "执行错误: " + e.getMessage();
        }
    }

    public static String execAddr2line(File soFile, String address, File toolFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    toolFile.getAbsolutePath(),
                    "-e", soFile.getAbsolutePath(),
                    "-f", "-C", "-p",
                    address
            );

            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                return "解析失败，退出码: " + exitCode;
            }
        } catch (Exception e) {
            return "执行错误: " + e.getMessage();
        }
    }
}
