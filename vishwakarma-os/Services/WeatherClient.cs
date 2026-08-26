using System.Net.Http;
using System.Text.Json.Nodes;

namespace PaperDesk_Companion.Services;

public static class WeatherClient
{
    public const double Lat = 15.2693;
    public const double Lon = 76.3871;
    public const string City = "Hospet, Karnataka";

    private const string Url =
        "https://api.open-meteo.com/v1/forecast?latitude=15.2693&longitude=76.3871" +
        "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
        "&daily=temperature_2m_max,temperature_2m_min&timezone=Asia%2FKolkata";

    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(8) };
    private static JsonObject? _cache;
    private static long _cacheAt;

    public static JsonObject? Fetch(bool force = false)
    {
        var now = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        if (!force && _cache is not null && now - _cacheAt < 15 * 60 * 1000) return _cache;
        try
        {
            var raw = Http.GetStringAsync(Url).GetAwaiter().GetResult();
            var src = JsonNode.Parse(raw) as JsonObject;
            if (src is null) return _cache;
            var snap = Snapshot(src);
            if (snap is null) return _cache;
            _cache = snap;
            _cacheAt = now;
            return snap;
        }
        catch
        {
            return _cache;
        }
    }

    public static string? PushToPhone(JsonObject bundle)
    {
        var weather = Fetch();
        if (weather is null) return null;
        bundle["weather"] = weather.DeepClone();
        var local = Path.Combine(Paths.CacheDir(), "weather.json");
        File.WriteAllText(local, weather.Pretty());
        AdbClient.Shell("mkdir -p " + Paths.RemoteRoot + "/inbox");
        AdbClient.Push(local, Paths.RemoteWeather);
        AdbClient.Push(local, Paths.RemoteWeatherInbox);
        AdbClient.Broadcast("com.paperdesk.os.APPLY_WEATHER");
        return weather.Str("condition", "Hospet");
    }

    private static JsonObject? Snapshot(JsonObject src)
    {
        try
        {
            var cur = src.Obj("current");
            var daily = src.Obj("daily");
            var high = daily["temperature_2m_max"] is JsonArray hi ? hi[0]?.GetValue<double>() ?? 0 : 0;
            var low = daily["temperature_2m_min"] is JsonArray lo ? lo[0]?.GetValue<double>() ?? 0 : 0;
            var code = cur.Int("weather_code");
            var isDay = cur.Int("is_day", 1) == 1;
            var now = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            return new JsonObject
            {
                ["source"] = "usb",
                ["city"] = City,
                ["tempC"] = cur.Num("temperature_2m"),
                ["highC"] = high,
                ["lowC"] = low,
                ["humidity"] = cur.Int("relative_humidity_2m"),
                ["windKmh"] = cur.Num("wind_speed_10m"),
                ["condition"] = Condition(code, isDay),
                ["kind"] = Kind(code),
                ["isDay"] = isDay,
                ["moonPhase"] = MoonPhase(now),
                ["updatedAt"] = now
            };
        }
        catch
        {
            return null;
        }
    }

    private static double Num(this JsonObject o, string key)
    {
        if (!o.TryGetPropertyValue(key, out var n) || n is not JsonValue v) return 0;
        if (v.TryGetValue<double>(out var d)) return d;
        if (v.TryGetValue<int>(out var i)) return i;
        return 0;
    }

    private static string Kind(int code) => code switch
    {
        0 or 1 => "SUN",
        2 => "CLOUD_SUN",
        3 => "CLOUD",
        45 or 48 => "FOG",
        51 or 53 or 55 or 56 or 57 or 61 or 63 or 65 or 66 or 67 or 80 or 81 or 82 => "RAIN",
        71 or 73 or 75 or 77 or 85 or 86 => "SNOW",
        95 or 96 or 99 => "STORM",
        _ => "CLOUD"
    };

    private static string Condition(int code, bool isDay)
    {
        if (!isDay && (code == 0 || code == 1)) return MoonName(MoonPhase());
        return code switch
        {
            0 => "Clear sky",
            1 => "Mainly clear",
            2 => "Partly cloudy",
            3 => "Overcast",
            45 or 48 => "Fog",
            51 or 53 or 55 => "Drizzle",
            56 or 57 => "Freezing drizzle",
            61 or 63 or 65 => "Rain",
            66 or 67 => "Freezing rain",
            71 or 73 or 75 or 77 => "Snow",
            80 or 81 or 82 => "Rain showers",
            85 or 86 => "Snow showers",
            95 => "Thunderstorm",
            96 or 99 => "Thunderstorm with hail",
            _ => "Hospet"
        };
    }

    private static float MoonPhase(long atMs = 0)
    {
        if (atMs == 0) atMs = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        const double synodic = 29.530588853;
        var jd = atMs / 86_400_000.0 + 2_440_587.5;
        const double knownNew = 2_451_550.1;
        var age = ((jd - knownNew) % synodic + synodic) % synodic;
        return (float)(age / synodic);
    }

    private static string MoonName(float phase)
    {
        var p = ((phase % 1f) + 1f) % 1f;
        if (p < 0.03f || p >= 0.97f) return "New moon";
        if (p < 0.22f) return "Waxing crescent";
        if (p < 0.28f) return "First quarter";
        if (p < 0.47f) return "Waxing gibbous";
        if (p < 0.53f) return "Full moon";
        if (p < 0.72f) return "Waning gibbous";
        if (p < 0.78f) return "Last quarter";
        return "Waning crescent";
    }
}
