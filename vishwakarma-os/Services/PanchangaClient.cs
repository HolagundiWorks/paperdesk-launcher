using System.Globalization;
using System.Net.Http;
using System.Text.Json.Nodes;
using System.Text.RegularExpressions;

namespace PaperDesk_Companion.Services;

public static class PanchangaClient
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(12) };

    public static JsonObject? Fetch(DateTime? day = null)
    {
        var date = (day ?? DateTime.Now).ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        try
        {
            var html = Http.GetStringAsync("https://srsmatha.org/panchanga.php?pdate=" + date).GetAwaiter().GetResult();
            return ParseSrs(html, date);
        }
        catch
        {
            return null;
        }
    }

    public static string? PushToPhone()
    {
        var day = Fetch();
        if (day is null) return null;
        var local = Path.Combine(Paths.CacheDir(), "panchanga.json");
        File.WriteAllText(local, day.Pretty());
        AdbClient.Shell("mkdir -p " + Paths.RemoteRoot + "/inbox");
        AdbClient.Push(local, Paths.RemotePanchanga);
        AdbClient.Push(local, Paths.RemotePanchangaInbox);
        return day.Str("tithi", "SRS MANTRALAYA");
    }

    public static JsonObject? ParseSrs(string html, string date)
    {
        var m = Regex.Match(html, "<div><p>(.*?)</p></div>", RegexOptions.IgnoreCase | RegexOptions.Singleline);
        if (!m.Success) return null;
        var t = Regex.Replace(m.Groups[1].Value, "<[^>]+>", " ");
        t = t.Replace("&nbsp;", " ");
        t = Regex.Replace(t, @"[,\s]+", " ").Trim();
        if (!t.Contains("Tithou", StringComparison.OrdinalIgnoreCase)) return null;
        var tithi = Cap(Regex.Match(t, @"(\S+)\s+Shubha\s+Tithou", RegexOptions.IgnoreCase).Groups[1].Value);
        var masa = Cap(Regex.Match(t, @"(\S+?)Mase", RegexOptions.IgnoreCase).Groups[1].Value).Replace("SHRAAVANA", "SHRAVANA");
        if (string.IsNullOrWhiteSpace(tithi) || string.IsNullOrWhiteSpace(masa)) return null;
        var ayana = Cap(Regex.Match(t, @"(Dakshinayaana|Uttarayana\w*)", RegexOptions.IgnoreCase).Groups[1].Value)
            .Replace("DAKSHINAYAANA", "DAKSHINAYANA").Replace("UTTARAYANA", "UTTARAYANA");
        return new JsonObject
        {
            ["date"] = date,
            ["samvatsara"] = Cap(Regex.Match(t, @"Sri\s+(\S+?)samvatsare", RegexOptions.IgnoreCase).Groups[1].Value),
            ["ayana"] = ayana,
            ["ritu"] = Cap(Regex.Match(t, @"(\S+?)Rutu", RegexOptions.IgnoreCase).Groups[1].Value),
            ["masa"] = masa,
            ["paksha"] = Cap(Regex.Match(t, @"(Shukla|Krishna)Pakshe", RegexOptions.IgnoreCase).Groups[1].Value),
            ["tithi"] = tithi,
            ["vara"] = Cap(Regex.Match(t, @"(Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday)").Groups[1].Value),
            ["nakshatra"] = Cap(Regex.Match(t, @"(\S+)\s+Nakshatre", RegexOptions.IgnoreCase).Groups[1].Value).Replace("SHRAAVANA", "SHRAVANA"),
            ["yoga"] = Cap(Regex.Match(t, @"(\S+)\s+Yoge", RegexOptions.IgnoreCase).Groups[1].Value),
            ["karana"] = Cap(Regex.Match(t, @"(\S+)\s+Karana", RegexOptions.IgnoreCase).Groups[1].Value),
            ["source"] = "SRS MANTRALAYA"
        };
    }

    private static string Cap(string raw)
    {
        if (string.IsNullOrWhiteSpace(raw)) return "";
        return raw.Replace("-", " ").ToUpperInvariant();
    }
}
