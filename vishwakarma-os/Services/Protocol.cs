using System.Globalization;
using System.Text.Json.Nodes;

namespace PaperDesk_Companion.Services;

public static class Paths
{
    public const string RemoteRoot = "/sdcard/Vishwakarma";
    public const string RemoteBundle = RemoteRoot + "/os-bundle.json";
    public const string RemoteInbox = RemoteRoot + "/inbox/os-bundle.json";
    public const string RemoteSettings = RemoteRoot + "/inbox/settings.json";
    public const string RemoteWeather = RemoteRoot + "/weather.json";
    public const string RemoteWeatherInbox = RemoteRoot + "/inbox/weather.json";
    public const string RemoteDevice = RemoteRoot + "/device.json";
    public const string RemoteSyncRequest = RemoteRoot + "/sync-request.json";
    public const string RemotePanchanga = RemoteRoot + "/panchanga.json";
    public const string RemotePanchangaInbox = RemoteRoot + "/inbox/panchanga.json";
    public const string RemoteProjects = RemoteRoot + "/Projects";

    public static string CacheDir()
    {
        var d = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".paperdesk-companion");
        Directory.CreateDirectory(d);
        return d;
    }

    public static string LocalBundle() => Path.Combine(CacheDir(), "os-bundle.json");
}

public static class MeasureMath
{
    private static readonly CultureInfo Us = CultureInfo.InvariantCulture;

    public static string Kind(string length, string breadth, string height, bool auto, string stored)
    {
        if (!auto) return stored;
        if (Filled(height)) return "VOL";
        if (Filled(breadth)) return "AREA";
        return "RUN";
    }

    public static double Qty(string length, string breadth, string height, string kind)
    {
        var l = Parse(length);
        var b = Parse(breadth);
        var h = Parse(height);
        return kind switch
        {
            "RUN" => l,
            "VOL" => l * b * h,
            _ => l * b
        };
    }

    public static string Unit(string kind, string uom)
    {
        var baseUnit = string.IsNullOrWhiteSpace(uom) ? "m" : uom;
        return kind switch
        {
            "RUN" => baseUnit,
            "VOL" => baseUnit + "3",
            _ => baseUnit + "2"
        };
    }

    public static string Label(string length, string breadth, string height, string uom, bool auto, string stored)
    {
        var k = Kind(length, breadth, height, auto, stored);
        return $"{Qty(length, breadth, height, k).ToString("#,##0.00", Us)} {Unit(k, uom)}";
    }

    public static string Totals(IEnumerable<JsonObject> rows)
    {
        double runM = 0, areaM2 = 0, volM3 = 0;
        foreach (var o in rows)
        {
            var auto = o.Flag("autoKind", true);
            var k = Kind(o.Str("length"), o.Str("breadth"), o.Str("height"), auto, o.Str("kind", "AREA"));
            var q = Qty(o.Str("length"), o.Str("breadth"), o.Str("height"), k);
            var m = o.Str("uom", "M") switch
            {
                "MM" => 0.001,
                "CM" => 0.01,
                "FT" => 0.3048,
                "IN" => 0.0254,
                _ => 1.0
            };
            switch (k)
            {
                case "RUN": runM += q * m; break;
                case "AREA": areaM2 += q * m * m; break;
                default: volM3 += q * m * m * m; break;
            }
        }

        var parts = new List<string>();
        if (runM != 0) parts.Add($"{runM.ToString("#,##0.00", Us)} m");
        if (areaM2 != 0) parts.Add($"{areaM2.ToString("#,##0.00", Us)} m2");
        if (volM3 != 0) parts.Add($"{volM3.ToString("#,##0.00", Us)} m3");
        return parts.Count == 0 ? "" : string.Join("   ", parts);
    }

    private static bool Filled(string s) => Parse(s) != 0 || !string.IsNullOrWhiteSpace(s);
    private static double Parse(string s) => double.TryParse(s, NumberStyles.Any, Us, out var n) ? n : 0;
}

public static class BundleFactory
{
    public static readonly HashSet<string> RequiredPackages =
    [
        "android", "com.android.systemui", "com.android.phone", "com.android.settings",
        "com.android.providers.settings", "com.android.shell", "com.paperdesk.launcher",
        "com.topjohnwu.magisk", "eu.chainfire.supersu",
        "com.google.android.packageinstaller", "com.android.packageinstaller",
        "com.samsung.android.packageinstaller"
    ];

    public static JsonObject Empty() => new()
    {
        ["version"] = 1,
        ["os"] = "AcharyaOS",
        ["osVersion"] = "2.0",
        ["projects"] = new JsonArray(),
        ["schedules"] = new JsonArray(),
        ["todos"] = new JsonArray(),
        ["notes"] = new JsonArray(),
        ["minutes"] = new JsonArray(),
        ["measurements"] = new JsonArray(),
        ["alarms"] = new JsonArray(),
        ["calculatorHistory"] = new JsonArray(),
        ["weather"] = new JsonObject(),
        ["settings"] = new JsonObject
        {
            ["pages"] = new JsonArray { "desk", "schedule", "grantha" },
            ["landscape"] = true,
            ["theme"] = "paper",
            ["accent"] = "#FF6A00",
            ["statusBar"] = false,
            ["navBar"] = false,
            ["alwaysOn"] = false,
            ["clockAnalog"] = true,
            ["projectEnabled"] = false,
            ["measureEnabled"] = false,
            ["calcEnabled"] = false,
            ["todoEnabled"] = false,
            ["scheduleEnabled"] = true,
            ["notesEnabled"] = false,
            ["gitaEnabled"] = false,
            ["notifications"] = true,
            ["usbDebug"] = true,
            ["developerOptions"] = true
        }
    };

    public static JsonObject ArchitectureProfile() => new()
    {
        ["name"] = "Acharya Mode",
        ["landscape"] = true,
        ["theme"] = "paper",
        ["notifications"] = false,
        ["projectEnabled"] = false,
        ["measureEnabled"] = false,
        ["calcEnabled"] = false,
        ["todoEnabled"] = false,
        ["scheduleEnabled"] = true,
        ["notesEnabled"] = false,
        ["gitaEnabled"] = false,
        ["pages"] = new JsonArray { "desk", "schedule", "grantha" },
        ["sound"] = false,
        ["vibration"] = false,
        ["lockScreen"] = false
    };

    public static string ProjectName(JsonObject bundle, string id)
    {
        if (string.IsNullOrWhiteSpace(id)) return "";
        return bundle.Arr("projects").Objects().FirstOrDefault(p => p.Str("id") == id)?.Str("name") ?? "";
    }
}
