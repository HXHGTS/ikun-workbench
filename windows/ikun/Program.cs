using Microsoft.Web.WebView2.Core;

namespace Ikun;

internal static class Program {
    [STAThread]
    private static void Main() {
        ApplicationConfiguration.Initialize();
        var form = new Form {
            Text = "ikun新媒体制作中心",
            Width = 1380, Height = 920,
            StartPosition = FormStartPosition.CenterScreen
        };
        var wv = new Microsoft.Web.WebView2.WinForms.WebView2 { Dock = DockStyle.Fill };
        form.Controls.Add(wv);
        /* 注意：Main 必须同步 void 才能让 [STAThread] 生效；
           async Task Main 会被 Roslyn 生成 MTA 入口 → WebView2 报 RPC_E_CHANGED_MODE */
        form.Shown += (_, _) => _ = InitWebViewAsync(form, wv);
        Application.Run(form);
    }

    private static async Task InitWebViewAsync(Form form, Microsoft.Web.WebView2.WinForms.WebView2 wv) {
        try {
            var opts = new CoreWebView2EnvironmentOptions {
                /* 本地单用户工具：放宽同源策略，五平台 API 直连 */
                AdditionalBrowserArguments = "--disable-web-security"
            };
            string udf = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "ikun-workbench");
            var env = await CoreWebView2Environment.CreateAsync(null, udf, opts);
            await wv.EnsureCoreWebView2Async(env);
            string www = Path.Combine(AppContext.BaseDirectory, "www");
            if (!Directory.Exists(www)) Directory.CreateDirectory(www);
            wv.CoreWebView2.SetVirtualHostNameToFolderMapping(
                "app.local", www, CoreWebView2HostResourceAccessKind.Allow);
            wv.Source = new Uri("https://app.local/index.html");
        } catch (WebView2RuntimeNotFoundException rnfe) {
            MessageBox.Show(
                "未检测到 Microsoft Edge WebView2 运行时。\nWin11 一般自带；若被精简请下载安装：\n" +
                "https://developer.microsoft.com/microsoft-edge/webview2\n\n" + rnfe.Message,
                "缺少 WebView2 运行时", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            try { System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(
                "https://developer.microsoft.com/microsoft-edge/webview2") { UseShellExecute = true }); } catch { }
            form.Close();
        } catch (DllNotFoundException dnfe) {
            MessageBox.Show("WebView2Loader.dll 加载失败：\n" + dnfe.Message +
                "\n\n请确保 ikun.exe 与 WebView2Loader.dll（runtimes 文件夹）在同一目录运行，不要单独拷贝 exe。",
                "启动失败", MessageBoxButtons.OK, MessageBoxIcon.Error);
            form.Close();
        } catch (Exception ex) {
            MessageBox.Show(ex.ToString(), "启动失败（完整信息）",
                MessageBoxButtons.OK, MessageBoxIcon.Error);
            form.Close();
        }
    }
}
