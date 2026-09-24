#!/usr/bin/env python3
"""tauri android init 生成 gen/android 后的注入脚本（Kotlin DSL 版）：
   1) ABI splits + universalApk（Kotlin DSL 语法）
   2) release 关闭 proguard（防 JNI 反射插件类被混淆裁掉）
   3) MainActivity.kt 通知权限请求"""
import re, sys, glob

gen = sys.argv[1] if len(sys.argv) > 1 else "gen/android"

kts_path = f"{gen}/app/build.gradle.kts"
src = open(kts_path, encoding="utf-8").read()

# 1) ABI splits（Kotlin DSL，注入到 kotlinOptions 之前）
if "splits {" not in src:
    splits = """    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

"""
    if "    kotlinOptions {" in src:
        src = src.replace("    kotlinOptions {", splits + "    kotlinOptions {", 1)
    else:
        src = src.replace("    buildTypes {", splits + "    buildTypes {", 1)
    print("splits 注入 ✓")

# 2) release 关 proguard
if "isMinifyEnabled = true" in src:
    src = src.replace("isMinifyEnabled = true", "isMinifyEnabled = false", 1)
    print("release proguard 关闭 ✓")

open(kts_path, "w", encoding="utf-8").write(src)

# 3) MainActivity.kt 通知权限请求
mk = glob.glob(f"{gen}/app/src/main/java/**/MainActivity.kt", recursive=True)
if mk:
    s2 = open(mk[0], encoding="utf-8").read()
    if "POST_NOTIFICATIONS" not in s2:
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
        s2 = re.sub(r'(class MainActivity\s*:\s*TauriActivity\(\)\s*\{)', r'\1\n' + inj, s2, count=1)
        open(mk[0], "w", encoding="utf-8").write(s2)
        print("MainActivity 权限注入 ✓")
    else:
        print("MainActivity 已注入，跳过")
else:
    print("MainActivity.kt 未找到（glob 失败）")
