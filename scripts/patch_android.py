#!/usr/bin/env python3
"""tauri android init 生成 gen/android 后的注入脚本：
   1) ABI splits + universalApk
   2) versionName 注入
   3) MainActivity.kt 通知权限请求"""
import re, sys, json

gen = sys.argv[1] if len(sys.argv) > 1 else "gen/android"
ver = sys.argv[2] if len(sys.argv) > 2 else None

gb = open(f"{gen}/app/build.gradle", encoding="utf-8").read()
# splits 注入（buildTypes 前）
if "splits {" not in gb:
    gb = gb.replace("    buildTypes {", """    splits {
        abi {
            enable true
            reset()
            include "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
            universalApk true
        }
    }
    buildTypes {""", 1)
# versionName 注入
if ver:
    gb = re.sub(r'versionName\s+"[^"]*"', f'versionName "{ver}"', gb, count=1)
open(f"{gen}/app/build.gradle", "w", encoding="utf-8").write(gb)
print("build.gradle: splits + versionName ✓")

# MainActivity.kt 权限请求
mk_path = f"{gen}/app/src/main/java"
import glob
mk = glob.glob(f"{mk_path}/**/MainActivity.kt", recursive=True)
if mk:
    src = open(mk[0], encoding="utf-8").read()
    if "POST_NOTIFICATIONS" not in src:
        inj = """    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.app.ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 700
            )
        }
    }
"""
        # 类体开头插入（class MainActivity : TauriActivity() { 之后）
        src = re.sub(r'(class MainActivity\s*:\s*TauriActivity\(\)\s*\{)', r'\1\n' + inj, src, count=1)
        open(mk[0], "w", encoding="utf-8").write(src)
        print("MainActivity.kt: 权限请求注入 ✓")
    else:
        print("MainActivity.kt: 已注入，跳过")
else:
    print("MainActivity.kt 未找到:", mk)
