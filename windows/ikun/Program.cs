using Microsoft.Web.WebView2.Core;

namespace Ikun;

internal static class Program {
    [STAThread]
    private static async Task Main() {
        ApplicationConfiguration.Initialize();
        var form = new Form {
            Text = "ikun新媒体制作中心",
            Width = 1380, Height = 920,
            StartPosition = FormStartPosition.CenterScreen
        };
        var wv = new Microsoft.Web.WebView2.WinForms.WebView2 { Dock = DockStyle.Fill };
        form.Controls.Add(wv);
        form.Shown += async (_, _) => {
            try {
                var opts = new CoreWebView2EnvironmentOptions {
                    /* 本地单用户工具：放宽同源策略，直连各平台 API */
                    AdditionalBrowserArguments = "--disable-web-security"
                };
                var env = await CoreWebView2Environment.CreateAsync(null, null, opts);
                await wv.EnsureCoreWebView2Async(env);
                string www = Path.Combine(AppContext.BaseDirectory, "www");
                if (!Directory.Exists(www)) Directory.CreateDirectory(www);
                wv.CoreWebView2.SetVirtualHostNameToFolderMapping(
                    "app.local", www, CoreWebView2HostResourceAccessKind.Allow);
                wv.CoreWebView2.Settings.AreDefaultContextMenusEnabled = true;
                wv.Source = new Uri("https://app.local/index.html");
            } catch (Exception ex) {
                MessageBox.Show(
                    "需要 Microsoft Edge WebView2 运行时（Win10/11 一般自带）。\n" +
                    "下载地址：https://developer.microsoft.com/microsoft-edge/webview2\n\n" + ex.Message,
                    "缺少 WebView2 运行时", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(
                    "https://developer.microsoft.com/microsoft-edge/webview2") { UseShellExecute = true });
                form.Close();
            }
        };
        Application.Run(form);
    }
}
