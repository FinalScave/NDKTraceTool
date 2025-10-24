# NDK堆栈还原工具
这是一个简单的GUI工具，用于快速还原Android native端崩溃堆栈，便于排查问题

## 截图

<img src="snapshot/scan-ndk.png">
<img src="snapshot/trace.png">

## 简要说明
Windows系统安装路径比较自由，故该工具扫描NDK路径也许扫描不到，可自行在`ToolSeacher`类中`scanCommonInstallPaths`方法加入自己的Android Sdk安装路径