using System.Globalization;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization.Metadata;

namespace PaperDesk_Companion.Services;

public static class JsonUtil
{
    public static string Str(this JsonObject o, string key, string fallback = "")
    {
        if (!o.TryGetPropertyValue(key, out var n) || n is null) return fallback;
        return n.GetValueKind() switch
        {
            System.Text.Json.JsonValueKind.String => n.GetValue<string>() ?? fallback,
            System.Text.Json.JsonValueKind.Null => fallback,
            _ => n.ToString()
        };
    }

    public static bool Flag(this JsonObject o, string key, bool fallback = false)
    {
        if (!o.TryGetPropertyValue(key, out var n) || n is not JsonValue v) return fallback;
        if (v.TryGetValue<bool>(out var b)) return b;
        if (v.TryGetValue<int>(out var i)) return i != 0;
        if (v.TryGetValue<string>(out var s))
            return s.Equals("true", StringComparison.OrdinalIgnoreCase) || s == "1";
        return fallback;
    }

    public static int Int(this JsonObject o, string key, int fallback = 0)
    {
        if (!o.TryGetPropertyValue(key, out var n) || n is not JsonValue v) return fallback;
        if (v.TryGetValue<int>(out var i)) return i;
        if (v.TryGetValue<long>(out var l)) return (int)l;
        if (v.TryGetValue<string>(out var s) && int.TryParse(s, out var p)) return p;
        return fallback;
    }

    public static long Long(this JsonObject o, string key, long fallback = 0)
    {
        if (!o.TryGetPropertyValue(key, out var n) || n is not JsonValue v) return fallback;
        if (v.TryGetValue<long>(out var l)) return l;
        if (v.TryGetValue<int>(out var i)) return i;
        if (v.TryGetValue<string>(out var s) && long.TryParse(s, out var p)) return p;
        return fallback;
    }

    public static JsonArray Arr(this JsonObject o, string key)
    {
        if (o[key] is JsonArray a) return a;
        var created = new JsonArray();
        o[key] = created;
        return created;
    }

    public static JsonObject Obj(this JsonObject o, string key)
    {
        if (o[key] is JsonObject nested) return nested;
        var created = new JsonObject();
        o[key] = created;
        return created;
    }

    public static IEnumerable<JsonObject> Objects(this JsonArray a) =>
        a.OfType<JsonObject>();

    public static JsonObject? FindById(this JsonArray a, string id) =>
        a.Objects().FirstOrDefault(o => o.Str("id") == id);

    public static string Pretty(this JsonNode node) =>
        node.ToJsonString(new JsonSerializerOptions
        {
            WriteIndented = true,
            TypeInfoResolver = new DefaultJsonTypeInfoResolver()
        });

    public static string FmtBytes(long n) => n switch
    {
        >= 1_000_000_000 => string.Format(CultureInfo.InvariantCulture, "{0:0.0} GB", n / 1_000_000_000.0),
        >= 1_000_000 => string.Format(CultureInfo.InvariantCulture, "{0:0.0} MB", n / 1_000_000.0),
        >= 1_000 => string.Format(CultureInfo.InvariantCulture, "{0:0.0} KB", n / 1_000.0),
        _ => $"{n} B"
    };

    public static string Stamp(long ms = 0)
    {
        var t = ms == 0 ? DateTime.Now : DateTimeOffset.FromUnixTimeMilliseconds(ms).LocalDateTime;
        return t.ToString("yyyyMMdd-HHmmss", CultureInfo.InvariantCulture);
    }

    public static string DisplayTime(long ms)
    {
        if (ms <= 0) return "";
        return DateTimeOffset.FromUnixTimeMilliseconds(ms).LocalDateTime.ToString("d MMM yyyy HH:mm", CultureInfo.InvariantCulture);
    }

    public static string Clock(int hour, int minute) =>
        $"{hour:00}:{minute:00}";
}
