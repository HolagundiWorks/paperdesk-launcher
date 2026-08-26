using System.Diagnostics;
using System.Text;
using System.Text.Json.Nodes;

namespace PaperDesk_Companion.Services;

public static class AdbClient
{
    public static string Binary()
    {
        var env = Environment.GetEnvironmentVariable("ANDROID_HOME")
            ?? Environment.GetEnvironmentVariable("ANDROID_SDK_ROOT");
        if (!string.IsNullOrWhiteSpace(env))
        {
            var win = Path.Combine(env, "platform-tools", "adb.exe");
            var unix = Path.Combine(env, "platform-tools", "adb");
            if (File.Exists(win)) return win;
            if (File.Exists(unix)) return unix;
        }

        var local = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Android", "Sdk", "platform-tools", "adb.exe");
        return File.Exists(local) ? local : "adb";
    }

    public static string Run(IEnumerable<string> args, int timeoutSec = 20)
    {
        var psi = new ProcessStartInfo
        {
            FileName = Binary(),
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true,
        };
        foreach (var a in args) psi.ArgumentList.Add(a);

        using var p = Process.Start(psi);
        if (p is null) return "adb failed to start";

        var output = new StringBuilder();
        p.OutputDataReceived += (_, e) => { if (e.Data != null) output.AppendLine(e.Data); };
        p.ErrorDataReceived += (_, e) => { if (e.Data != null) output.AppendLine(e.Data); };
        p.BeginOutputReadLine();
        p.BeginErrorReadLine();

        if (!p.WaitForExit(timeoutSec * 1000))
        {
            try { p.Kill(true); } catch { /* ignore */ }
            return output + "\n(timeout)";
        }

        return output.ToString().Trim();
    }

    public static string Run(params string[] args) => Run(args, 20);

    public static string? Serial()
    {
        var lines = Run("devices").Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        return lines.Skip(1).FirstOrDefault(l => l.Contains("\tdevice"))?.Split('\t')[0];
    }

    public static bool Connected() => Serial() != null;

    public static string Shell(string cmd) => Run("shell", cmd);

    public static string Pull(string remote, string local)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(local)!);
        return Run("pull", remote, local);
    }

    public static string Push(string local, string remote) => Run("push", local, remote);

    public static string Broadcast(string action, IDictionary<string, string>? extras = null)
    {
        var args = new List<string>
        {
            "shell", "am", "broadcast",
            "-n", "com.paperdesk.launcher/.bridge.OsBridgeReceiver",
            "-a", action
        };
        if (extras != null)
        {
            foreach (var (k, v) in extras)
            {
                args.Add("--es");
                args.Add(k);
                args.Add(v);
            }
        }
        return Run(args);
    }

    public static void ApplyRadios(JsonObject s)
    {
        if (s.Flag("usbDebug", true))
        {
            Shell("settings put global adb_enabled 1");
            Shell("settings put global development_settings_enabled 1");
        }
    }
}
