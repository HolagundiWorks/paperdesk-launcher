using System.Collections.ObjectModel;
using System.Globalization;
using System.IO.Compression;
using System.Text.Json.Nodes;
using System.Threading;
using PaperDesk_Companion.Models;

namespace PaperDesk_Companion.Services;

public sealed class OsSession
{
    public JsonObject Bundle { get; private set; } = BundleFactory.Empty();
    public string Status { get; private set; } = "USB disconnected";
    public bool Connected { get; private set; }
    public string ConnectionTitle => Connected ? "USB connected" : "USB disconnected";
    public string Search { get; set; } = "";
    public string? SelectedProjectId { get; set; }
    public long LastSync { get; private set; }
    public string DashboardText { get; private set; } = "Connect a phone over USB to sync.";
    public string MeasureTotal { get; private set; } = "";
    public string PagesText { get; set; } = "desk schedule grantha";
    public string SettingsJson { get; set; } = "{}";
    public string FileListing { get; private set; } = "";
    public string AdvancedLog { get; private set; } = "";
    public string SelectedFileProject { get; set; } = "Project";
    public string SelectedFileFolder { get; set; } = "Photos";
    private int _syncing;
    private CancellationTokenSource? _watch;

    public ObservableCollection<ProjectRow> Projects { get; } = [];
    public ObservableCollection<ScheduleRow> Schedules { get; } = [];
    public ObservableCollection<TodoRow> Todos { get; } = [];
    public ObservableCollection<TodoRow> TodosBacklog { get; } = [];
    public ObservableCollection<TodoRow> TodosDoing { get; } = [];
    public ObservableCollection<TodoRow> TodosDone { get; } = [];
    public ObservableCollection<NoteRow> Notes { get; } = [];
    public ObservableCollection<MinutesRow> Minutes { get; } = [];
    public ObservableCollection<MeasureRow> Measurements { get; } = [];
    public ObservableCollection<NoteRow> ProjectNotes { get; } = [];
    public ObservableCollection<MinutesRow> ProjectMinutes { get; } = [];
    public ObservableCollection<ScheduleRow> ProjectSchedules { get; } = [];
    public ObservableCollection<TodoRow> ProjectTodos { get; } = [];
    public ObservableCollection<TodoRow> ProjectTodosBacklog { get; } = [];
    public ObservableCollection<TodoRow> ProjectTodosDoing { get; } = [];
    public ObservableCollection<TodoRow> ProjectTodosDone { get; } = [];
    public ObservableCollection<AppRow> Apps { get; } = [];
    public ObservableCollection<SettingFlag> FeatureFlags { get; } = [];
    public ObservableCollection<SettingFlag> BackupFlags { get; } = [];
    public ObservableCollection<string> ProjectNames { get; } = [];
    public ObservableCollection<string> FileFolders { get; } =
        ["Measurements", "Notes", "Minutes", "Drawings", "PDFs", "Photos"];

    public event Action? Changed;

    private static readonly (string Key, string Label)[] FeatureDefs =
    [
        ("landscape", "Landscape launcher"),
        ("statusBar", "Status bar"),
        ("navBar", "Navigation bar"),
        ("alwaysOn", "Always-on display"),
        ("clockAnalog", "Analog clock"),
        ("calcScientific", "Scientific calculator"),
        ("mobileData", "Mobile network"),
        ("nfc", "NFC"),
        ("gps", "GPS"),
        ("sensors", "Sensors"),
        ("autoRotate", "Auto rotation"),
        ("usbDebug", "USB debugging"),
        ("developerOptions", "Developer options"),
        ("lockScreen", "Lock screen"),
        ("notifications", "Notifications"),
        ("sound", "Sound"),
        ("vibration", "Vibration"),
        ("projectEnabled", "Project"),
        ("calcEnabled", "Calculator"),
        ("todoEnabled", "Todo"),
        ("scheduleEnabled", "Schedule"),
        ("notesEnabled", "Notes"),
        ("gitaEnabled", "Geethavani"),
        ("measureEnabled", "Measurement Book")
    ];

    private static readonly (string Key, string Label)[] BackupDefs =
    [
        ("projects", "Projects"),
        ("schedules", "Schedules"),
        ("todos", "Todos"),
        ("notes", "Notes"),
        ("minutes", "Minutes"),
        ("measurements", "Measurements"),
        ("settings", "Settings / launcher / OS configuration"),
        ("alarms", "Alarms"),
        ("calculatorHistory", "Calculator history"),
        ("weather", "Weather (USB)")
    ];

    public OsSession()
    {
        foreach (var (k, l) in FeatureDefs) FeatureFlags.Add(new SettingFlag(k, l, false));
        foreach (var (k, l) in BackupDefs) BackupFlags.Add(new SettingFlag(k, l, true));
    }

    public void Initialize()
    {
        var cached = Paths.LocalBundle();
        if (File.Exists(cached))
        {
            try
            {
                Bundle = JsonNode.Parse(File.ReadAllText(cached)) as JsonObject ?? BundleFactory.Empty();
            }
            catch
            {
                Bundle = BundleFactory.Empty();
            }
        }
        Paint();
        _ = RefreshConnectionAsync();
        _watch?.Cancel();
        _watch = new CancellationTokenSource();
        _ = WatchPhoneSyncAsync(_watch.Token);
    }

    public void Raise() => Changed?.Invoke();

    public void Paint()
    {
        try
        {
            PaintCore();
        }
        catch (Exception ex)
        {
            File.WriteAllText(Path.Combine(Paths.CacheDir(), "companion-crash.txt"), DateTime.Now + "\nPaint\n" + ex);
            throw;
        }
    }

    private void PaintCore()
    {
        var q = Search.Trim().ToLowerInvariant();
        bool Match(string s) => string.IsNullOrEmpty(q) || s.ToLowerInvariant().Contains(q);

        Projects.Clear();
        Schedules.Clear();
        Todos.Clear();
        TodosBacklog.Clear();
        TodosDoing.Clear();
        TodosDone.Clear();
        Notes.Clear();
        Minutes.Clear();
        Measurements.Clear();
        ProjectNotes.Clear();
        ProjectMinutes.Clear();
        ProjectSchedules.Clear();
        ProjectTodos.Clear();
        ProjectTodosBacklog.Clear();
        ProjectTodosDoing.Clear();
        ProjectTodosDone.Clear();
        ProjectNames.Clear();

        foreach (var p in Bundle.Arr("projects").Objects())
        {
            var name = p.Str("name");
            if (!string.IsNullOrWhiteSpace(name)) ProjectNames.Add(name);
            if (!Match(name + p.Str("number"))) continue;
            var id = p.Str("id");
            Projects.Add(new ProjectRow
            {
                Id = id,
                Name = name,
                Number = p.Str("number"),
                Updated = JsonUtil.DisplayTime(p.Long("updatedAt")),
                Measurements = Bundle.Arr("measurements").Objects().Count(m => m.Str("projectId") == id),
                Notes = Bundle.Arr("notes").Objects().Count(m => m.Str("projectId") == id),
                Minutes = Bundle.Arr("minutes").Objects().Count(m => m.Str("projectId") == id),
                Schedules = Bundle.Arr("schedules").Objects().Count(m => m.Str("projectId") == id),
                Todos = Bundle.Arr("todos").Objects().Count(m => m.Str("projectId") == id)
            });
        }

        foreach (var e in Bundle.Arr("schedules").Objects().OrderBy(x => x.Int("hour") * 60 + x.Int("minute")))
        {
            var pname = BundleFactory.ProjectName(Bundle, e.Str("projectId"));
            var title = e.Str("title");
            if (!Match(title + pname)) continue;
            var row = new ScheduleRow
            {
                Id = e.Str("id"),
                Time = JsonUtil.Clock(e.Int("hour"), e.Int("minute")),
                Title = title,
                Project = string.IsNullOrWhiteSpace(pname) ? "" : $"[{pname}]",
                Date = JsonUtil.DisplayTime(e.Long("dayStart")),
                Notes = e.Str("notes"),
                Done = e.Flag("done")
            };
            Schedules.Add(row);
            if (e.Str("projectId") == SelectedProjectId) ProjectSchedules.Add(row);
        }

        foreach (var t in Bundle.Arr("todos").Objects())
        {
            var pname = BundleFactory.ProjectName(Bundle, t.Str("projectId"));
            if (string.IsNullOrWhiteSpace(pname)) pname = t.Str("project");
            var title = t.Str("text");
            if (!Match(title + pname)) continue;
            var lane = TodoLane(t);
            var row = new TodoRow
            {
                Id = t.Str("id"),
                Done = lane == "done",
                Lane = lane,
                Title = title,
                Project = string.IsNullOrWhiteSpace(pname) ? "" : $"[{pname}]",
                Due = JsonUtil.DisplayTime(t.Long("dueAt")),
                Priority = t.Int("priority", 1),
                Notes = t.Str("notes")
            };
            Todos.Add(row);
            LaneList(false, lane).Add(row);
            if (t.Str("projectId") == SelectedProjectId)
            {
                ProjectTodos.Add(row);
                LaneList(true, lane).Add(row);
            }
        }

        foreach (var n in Bundle.Arr("notes").Objects())
        {
            var pname = BundleFactory.ProjectName(Bundle, n.Str("projectId"));
            var title = n.Str("title");
            if (string.IsNullOrWhiteSpace(title)) title = n.Str("body");
            if (string.IsNullOrWhiteSpace(title) && !string.IsNullOrWhiteSpace(n.Str("photo"))) title = "PHOTO";
            if (!Match(title + pname + n.Str("body"))) continue;
            var row = new NoteRow
            {
                Id = n.Str("id"),
                Title = title,
                Project = string.IsNullOrWhiteSpace(pname) ? "" : $"[{pname}]",
                Updated = JsonUtil.DisplayTime(n.Long("updatedAt")),
                Body = n.Str("body"),
                Photo = n.Str("photo")
            };
            Notes.Add(row);
            if (n.Str("projectId") == SelectedProjectId) ProjectNotes.Add(row);
        }

        foreach (var m in Bundle.Arr("minutes").Objects().OrderByDescending(x => x.Long("dayStart")))
        {
            var pname = BundleFactory.ProjectName(Bundle, m.Str("projectId"));
            var title = m.Str("title");
            if (string.IsNullOrWhiteSpace(title)) title = m.Str("body");
            if (!Match(title + pname + m.Str("attendees") + m.Str("body"))) continue;
            var row = new MinutesRow
            {
                Id = m.Str("id"),
                Title = title,
                Project = string.IsNullOrWhiteSpace(pname) ? "" : $"[{pname}]",
                Date = JsonUtil.DisplayTime(m.Long("dayStart")),
                Attendees = m.Str("attendees"),
                Body = m.Str("body"),
                Photo = m.Str("photo")
            };
            Minutes.Add(row);
            if (m.Str("projectId") == SelectedProjectId) ProjectMinutes.Add(row);
        }

        var pid = SelectedProjectId;
        var scoped = new List<JsonObject>();
        var itemNo = 0;
        foreach (var m in Bundle.Arr("measurements").Objects())
        {
            if (string.IsNullOrWhiteSpace(pid) || m.Str("projectId") != pid) continue;
            var pname = BundleFactory.ProjectName(Bundle, m.Str("projectId"));
            var desc = m.Str("description");
            if (!Match(desc + pname)) continue;
            scoped.Add(m);
            itemNo++;
            var auto = m.Flag("autoKind", true);
            var kind = MeasureMath.Kind(m.Str("length"), m.Str("breadth"), m.Str("height"), auto, m.Str("kind", "AREA"));
            Measurements.Add(new MeasureRow
            {
                Id = m.Str("id"),
                Item = itemNo,
                Project = string.IsNullOrWhiteSpace(pname) ? "" : $"[{pname}]",
                Description = desc,
                Length = m.Str("length"),
                Breadth = m.Str("breadth"),
                Height = m.Str("height"),
                Uom = m.Str("uom"),
                Kind = kind,
                Qty = MeasureMath.Label(m.Str("length"), m.Str("breadth"), m.Str("height"), m.Str("uom", "M").ToLowerInvariant(), auto, kind)
            });
        }
        MeasureTotal = string.IsNullOrEmpty(MeasureMath.Totals(scoped)) ? "" : "Total  " + MeasureMath.Totals(scoped);

        if (ProjectNames.Count == 0) ProjectNames.Add("Project");
        if (string.IsNullOrWhiteSpace(SelectedFileProject) || !ProjectNames.Contains(SelectedFileProject))
            SelectedFileProject = ProjectNames[0];

        PaintSettings();
        Raise();
    }

    public void PaintSettings()
    {
        var s = Bundle.Obj("settings");
        foreach (var flag in FeatureFlags)
            flag.IsOn = s.Flag(flag.Key, flag.IsOn);
        var pages = s["pages"] as JsonArray;
        PagesText = pages is null
            ? "desk schedule grantha"
            : string.Join(" ", pages.Select(n => n?.ToString()).Where(x => !string.IsNullOrWhiteSpace(x)));
        SettingsJson = s.Pretty();
    }

    public void CollectSettings()
    {
        JsonObject s;
        try { s = JsonNode.Parse(SettingsJson) as JsonObject ?? Bundle.Obj("settings"); }
        catch { s = Bundle.Obj("settings"); }
        foreach (var flag in FeatureFlags) s[flag.Key] = flag.IsOn;
        var pages = new JsonArray();
        foreach (var p in PagesText.Split(' ', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
            pages.Add(p);
        if (pages.Count > 0) s["pages"] = pages;
        Bundle["settings"] = s;
        SettingsJson = s.Pretty();
    }

    public void SaveLocal()
    {
        File.WriteAllText(Paths.LocalBundle(), Bundle.Pretty());
    }

    public JsonObject? Find(string key, string id) => Bundle.Arr(key).FindById(id);

    public void Upsert(string key, JsonObject obj)
    {
        var arr = Bundle.Arr(key);
        var id = obj.Str("id");
        for (var i = 0; i < arr.Count; i++)
        {
            if (arr[i] is JsonObject o && o.Str("id") == id)
            {
                arr[i] = obj;
                SaveLocal();
                return;
            }
        }
        arr.Add(obj);
        SaveLocal();
    }

    public void DeleteFrom(string key, string? id)
    {
        if (string.IsNullOrWhiteSpace(id)) return;
        var arr = Bundle.Arr(key);
        for (var i = arr.Count - 1; i >= 0; i--)
        {
            if (arr[i] is JsonObject o && o.Str("id") == id) arr.RemoveAt(i);
        }
        SaveLocal();
        Paint();
    }

    public void DeleteProject(string id)
    {
        DeleteFrom("projects", id);
        foreach (var key in new[] { "measurements", "notes", "minutes", "schedules", "todos" })
        {
            var arr = Bundle.Arr(key);
            for (var i = arr.Count - 1; i >= 0; i--)
            {
                if (arr[i] is JsonObject o && o.Str("projectId") == id) arr.RemoveAt(i);
            }
        }
        if (SelectedProjectId == id) SelectedProjectId = null;
        SaveLocal();
        Paint();
    }

    public string IdForName(string name)
    {
        if (string.IsNullOrWhiteSpace(name)) return "";
        var clean = name.Trim().TrimStart('[').TrimEnd(']');
        return Bundle.Arr("projects").Objects()
            .FirstOrDefault(p => p.Str("name").Equals(clean, StringComparison.OrdinalIgnoreCase))
            ?.Str("id") ?? "";
    }

    public string SelectedProjectName =>
        string.IsNullOrWhiteSpace(SelectedProjectId) ? "Project" : BundleFactory.ProjectName(Bundle, SelectedProjectId);

    public async Task<string> RunIoAsync(string message, Func<string> io)
    {
        Status = message;
        Raise();
        try
        {
            var result = await Task.Run(io);
            Status = result;
            Raise();
            return result;
        }
        catch (Exception ex)
        {
            Status = ex.Message;
            Raise();
            return ex.Message;
        }
    }

    public async Task SyncNowAsync()
    {
        if (Interlocked.CompareExchange(ref _syncing, 1, 0) != 0) return;
        try
        {
        Status = "Exporting from phone...";
        Raise();
        JsonObject? next = null;
        string msg;
        try
        {
            (next, msg) = await Task.Run(() =>
            {
                if (!AdbClient.Connected()) return ((JsonObject?)null, "Phone not connected");
                AdbClient.Broadcast("com.paperdesk.os.EXPORT");
                Thread.Sleep(900);
                AdbClient.Pull(Paths.RemoteBundle, Paths.LocalBundle());
                JsonObject parsed;
                try { parsed = JsonNode.Parse(File.ReadAllText(Paths.LocalBundle())) as JsonObject ?? BundleFactory.Empty(); }
                catch { parsed = BundleFactory.Empty(); }
                var label = WeatherClient.PushToPhone(parsed);
                File.WriteAllText(Paths.LocalBundle(), parsed.Pretty());
                var extra = string.IsNullOrWhiteSpace(label) ? "" : "  Weather: " + label;
                var pan = PanchangaClient.PushToPhone();
                if (!string.IsNullOrWhiteSpace(pan)) extra += "  Panchanga: " + pan;
                return (parsed, "Synced" + extra);
            });
        }
        catch (Exception ex)
        {
            msg = ex.Message;
        }

        if (next != null)
        {
            Bundle = next;
            LastSync = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            Paint();
            await RefreshConnectionAsync();
        }
        Status = msg;
        Raise();
        }
        finally
        {
            Interlocked.Exchange(ref _syncing, 0);
        }
    }

    private async Task WatchPhoneSyncAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            try
            {
                await Task.Delay(2000, ct);
            }
            catch (TaskCanceledException)
            {
                return;
            }
            if (!AdbClient.Connected()) continue;
            var stamp = AdbClient.Shell("cat " + Paths.RemoteSyncRequest + " 2>/dev/null");
            if (string.IsNullOrWhiteSpace(stamp) || stamp.Contains("No such")) continue;
            AdbClient.Shell("rm " + Paths.RemoteSyncRequest);
            await SyncNowAsync();
        }
    }

    public async Task PushAsync()
    {
        CollectSettings();
        SaveLocal();
        LastSync = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        await RunIoAsync("Pushing to phone...", () =>
        {
            if (!AdbClient.Connected()) return "Phone not connected";
            WeatherClient.PushToPhone(Bundle);
            PanchangaClient.PushToPhone();
            SaveLocal();
            AdbClient.Shell("mkdir -p " + Paths.RemoteRoot + "/inbox");
            AdbClient.Push(Paths.LocalBundle(), Paths.RemoteInbox);
            AdbClient.Broadcast("com.paperdesk.os.IMPORT");
            return "Pushed";
        });
    }

    public async Task ApplySettingsAsync()
    {
        CollectSettings();
        var obj = Bundle.Obj("settings");
        var f = Path.Combine(Paths.CacheDir(), "settings.json");
        File.WriteAllText(f, obj.Pretty());
        SaveLocal();
        await RunIoAsync("Applying settings...", () =>
        {
            if (!AdbClient.Connected()) return "Phone not connected";
            AdbClient.Shell("mkdir -p " + Paths.RemoteRoot + "/inbox");
            AdbClient.Push(f, Paths.RemoteSettings);
            AdbClient.Broadcast("com.paperdesk.os.APPLY_SETTINGS");
            AdbClient.ApplyRadios(obj);
            return "Settings applied";
        });
    }

    public void LoadArchitectureProfile()
    {
        var p = BundleFactory.ArchitectureProfile();
        var s = Bundle.Obj("settings");
        foreach (var kv in p)
            s[kv.Key] = kv.Value?.DeepClone();
        Bundle["settings"] = s;
        PaintSettings();
        Status = "Acharya Mode loaded. Apply to phone to use it.";
        Raise();
    }

    public void SaveProfile(string name)
    {
        CollectSettings();
        var s = Bundle.Obj("settings");
        s["name"] = name;
        var profiles = s["profiles"] as JsonArray ?? new JsonArray();
        profiles.Add(s.DeepClone());
        s["profiles"] = profiles;
        Bundle["settings"] = s;
        PaintSettings();
        Status = "Profile saved in the bundle. Push or apply to keep it on the phone.";
        Raise();
    }

    public Task BackupAsync(string destPath)
    {
        CollectSettings();
        var pack = new JsonObject
        {
            ["version"] = Bundle.Int("version", 1),
            ["os"] = Bundle.Str("os", "AcharyaOS"),
            ["osVersion"] = Bundle.Str("osVersion", "2.0"),
            ["exportedAt"] = DateTimeOffset.Now.ToUnixTimeMilliseconds()
        };
        foreach (var flag in BackupFlags.Where(f => f.IsOn))
        {
            if (flag.Key == "settings") pack["settings"] = Bundle.Obj("settings").DeepClone();
            else pack[flag.Key] = Bundle.Arr(flag.Key).DeepClone();
        }
        return RunIoAsync("Creating backup...", () =>
        {
            if (File.Exists(destPath)) File.Delete(destPath);
            using var zip = ZipFile.Open(destPath, ZipArchiveMode.Create);
            var entry = zip.CreateEntry("os-bundle.json");
            using var w = new StreamWriter(entry.Open());
            w.Write(pack.Pretty());
            return "Backup written: " + Path.GetFileName(destPath);
        });
    }

    public async Task RestoreAsync(string path, string mode)
    {
        Status = "Restoring...";
        Raise();
        JsonObject? incoming;
        string msg;
        try
        {
            incoming = await Task.Run(() =>
            {
                string raw;
                if (path.EndsWith(".zip", StringComparison.OrdinalIgnoreCase))
                {
                    using var zip = ZipFile.OpenRead(path);
                    var entry = zip.Entries.FirstOrDefault(e => e.FullName.EndsWith("os-bundle.json", StringComparison.OrdinalIgnoreCase));
                    if (entry is null) return null;
                    using var r = new StreamReader(entry.Open());
                    raw = r.ReadToEnd();
                }
                else raw = File.ReadAllText(path);
                return JsonNode.Parse(raw) as JsonObject;
            });
            if (incoming is null)
            {
                Status = "No os-bundle.json in backup";
                Raise();
                return;
            }
            switch (mode)
            {
                case "config":
                    Bundle["settings"] = incoming["settings"]?.DeepClone() ?? new JsonObject();
                    break;
                case "data":
                    foreach (var key in new[] { "projects", "schedules", "todos", "notes", "minutes", "measurements", "alarms", "calculatorHistory", "weather" })
                    {
                        if (incoming[key] != null) Bundle[key] = incoming[key]!.DeepClone();
                    }
                    break;
                default:
                    Bundle = incoming;
                    break;
            }
            SaveLocal();
            Paint();
            msg = await RunIoAsync("Restoring...", () =>
            {
                if (!AdbClient.Connected()) return "Restore complete (phone offline)";
                AdbClient.Shell("mkdir -p " + Paths.RemoteRoot + "/inbox");
                AdbClient.Push(Paths.LocalBundle(), Paths.RemoteInbox);
                AdbClient.Broadcast("com.paperdesk.os.IMPORT");
                return "Restore complete";
            });
        }
        catch (Exception ex)
        {
            msg = ex.Message;
        }
        Status = msg;
        Raise();
    }

    public Task InstallApkAsync(string apk) => RunIoAsync("Installing...", () =>
    {
        if (!AdbClient.Connected()) return "Phone not connected";
        return AdbClient.Run(["install", "-r", apk], 120);
    });

    public string RemoteProjectDir()
    {
        var name = string.IsNullOrWhiteSpace(SelectedFileProject) ? "Project" : SelectedFileProject;
        var folder = string.IsNullOrWhiteSpace(SelectedFileFolder) ? "Photos" : SelectedFileFolder;
        var safe = System.Text.RegularExpressions.Regex.Replace(name, "[^A-Za-z0-9._-]+", "_").Trim('_');
        if (string.IsNullOrWhiteSpace(safe)) safe = "Project";
        return $"{Paths.RemoteProjects}/{safe}/{folder}";
    }

    public async Task SendFileAsync(string localPath)
    {
        var dir = RemoteProjectDir();
        var remote = $"{dir}/{Path.GetFileName(localPath)}";
        await RunIoAsync("Sending file...", () =>
        {
            AdbClient.Shell("mkdir -p " + dir);
            AdbClient.Push(localPath, remote);
            return "Sent " + Path.GetFileName(localPath);
        });
        await LoadFilesAsync();
    }

    public Task RecvFileAsync(string remote, string localDir) => RunIoAsync("Receiving file...", () =>
    {
        AdbClient.Pull(remote, Path.Combine(localDir, Path.GetFileName(remote)));
        return "Pulled";
    });

    public async Task LoadFilesAsync()
    {
        var dir = RemoteProjectDir();
        var listing = await Task.Run(() =>
        {
            var outp = AdbClient.Connected() ? AdbClient.Shell("ls -la " + dir) : "Phone not connected";
            return dir + "\n\n" + outp;
        });
        FileListing = listing;
        Status = "OK";
        Raise();
    }

    public async Task LoadAppsAsync()
    {
        Status = "Reading packages...";
        Raise();
        if (!AdbClient.Connected())
        {
            Status = "Phone not connected";
            Raise();
            return;
        }

        var (all, third, disabled) = await Task.Run(() => (
            Packages("pm list packages"),
            Packages("pm list packages -3"),
            Packages("pm list packages -d")));

        Apps.Clear();
        foreach (var pkg in all.OrderBy(x => x, StringComparer.OrdinalIgnoreCase))
        {
            var kind = BundleFactory.RequiredPackages.Contains(pkg) ? "REQUIRED"
                : third.Contains(pkg) ? "USER" : "SYSTEM";
            Apps.Add(new AppRow
            {
                Name = pkg.Contains('.') ? pkg[(pkg.LastIndexOf('.') + 1)..] : pkg,
                Package = pkg,
                Kind = kind,
                Enabled = !disabled.Contains(pkg)
            });
        }
        Status = "OK";
        Raise();
    }

    private static HashSet<string> Packages(string cmd) =>
        AdbClient.Shell(cmd).Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(l => l.StartsWith("package:") ? l[8..] : l)
            .Where(l => !string.IsNullOrWhiteSpace(l))
            .ToHashSet();

    public async Task PkgActionAsync(string op, AppRow row)
    {
        if (row.Kind == "REQUIRED" && op is "disable" or "uninstall" or "clear")
        {
            Status = "Required OS component: " + row.Package;
            Raise();
            return;
        }
        if (op == "uninstall" && row.Kind != "USER")
        {
            Status = "Only user-installed apps can be uninstalled.";
            Raise();
            return;
        }
        await RunIoAsync("Package " + op + "...", () => op switch
        {
            "enable" => AdbClient.Shell("pm enable " + row.Package),
            "disable" => AdbClient.Shell("pm disable-user --user 0 " + row.Package),
            "uninstall" => AdbClient.Shell("pm uninstall " + row.Package),
            "clear" => AdbClient.Shell("pm clear " + row.Package),
            "open" => AdbClient.Shell($"monkey -p {row.Package} -c android.intent.category.LAUNCHER 1"),
            _ => "OK"
        });
        await LoadAppsAsync();
    }

    public async Task RunAdbAsync(string cmd)
    {
        var output = await Task.Run(() =>
        {
            var parts = cmd.Split(' ', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
            return AdbClient.Run(parts, 30);
        });
        AdvancedLog += $"> adb {cmd}\n{output}\n\n";
        Status = "OK";
        Raise();
    }

    public async Task RefreshConnectionAsync()
    {
        Status = "Reading device...";
        Raise();
        var snapshot = await Task.Run(ReadDevice);
        Connected = snapshot.Connected;
        DashboardText = snapshot.Text;
        if (Connected)
        {
            var wx = await Task.Run(() => WeatherClient.PushToPhone(Bundle));
            if (!string.IsNullOrWhiteSpace(wx))
            {
                SaveLocal();
                DashboardText = snapshot.Text + "\nWeather: " + wx + " (USB)";
            }
            var pan = await Task.Run(PanchangaClient.PushToPhone);
            if (!string.IsNullOrWhiteSpace(pan))
                DashboardText += "\nPanchanga: " + pan + " (USB)";
        }
        Status = snapshot.Connected ? "USB connected" : "USB disconnected";
        Raise();
    }

    private (bool Connected, string Text) ReadDevice()
    {
        var serial = AdbClient.Serial();
        var connected = serial != null;
        var lines = new List<string>();
        if (!connected)
        {
            lines.Add("Connection: USB disconnected");
        }
        else
        {
            lines.Add("Connection: USB connected  " + serial);
            var device = Bundle["device"] as JsonObject;
            lines.Add("Device: " + NonBlank(device?.Str("name"), () => AdbClient.Shell("getprop ro.product.model")));
            lines.Add("Android: " + NonBlank(device?.Str("android"), () => AdbClient.Shell("getprop ro.build.version.release")));
            lines.Add("Build: " + NonBlank(device?.Str("build"), () => AdbClient.Shell("getprop ro.build.display.id")));
            lines.Add("Battery: " + (device?["battery"]?.ToString() ?? AdbClient.Shell("dumpsys battery | grep level")));
            if (device != null)
            {
                lines.Add("Storage: " + JsonUtil.FmtBytes(device.Long("storageAvail")) + " free of " + JsonUtil.FmtBytes(device.Long("storageTotal")));
                lines.Add("Database size: " + JsonUtil.FmtBytes(device.Long("databaseBytes")));
            }
            else
            {
                lines.Add("Storage: " + AdbClient.Shell("df /data | tail -n 1"));
                lines.Add("Database size: " + JsonUtil.FmtBytes(Bundle.Pretty().Length));
            }
            lines.Add("OS: " + Bundle.Str("os", "AcharyaOS") + " " + Bundle.Str("osVersion", "2.0"));
            lines.Add("Last sync: " + (LastSync == 0 ? "never" : JsonUtil.Stamp(LastSync)));
            lines.Add("Backup: local cache " + Paths.LocalBundle());
        }
        return (connected, string.Join("\n", lines));
    }

    private static string NonBlank(string? value, Func<string> fallback) =>
        string.IsNullOrWhiteSpace(value) ? fallback() : value!;

    public void SaveProject(JsonObject obj)
    {
        obj["updatedAt"] = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        Upsert("projects", obj);
        Paint();
    }

    public void SaveSchedule(JsonObject obj)
    {
        obj["timeLabel"] = JsonUtil.Clock(obj.Int("hour"), obj.Int("minute"));
        Upsert("schedules", obj);
        Paint();
    }

    public void SaveTodo(JsonObject obj)
    {
        var lane = TodoLane(obj);
        obj["status"] = lane;
        obj["done"] = lane == "done";
        Upsert("todos", obj);
        Paint();
    }

    public void SetTodoStatus(string id, string lane)
    {
        var obj = Find("todos", id);
        if (obj is null) return;
        var next = NormalizeLane(lane);
        if (TodoLane(obj) == next) return;
        obj["status"] = next;
        obj["done"] = next == "done";
        SaveLocal();
        Paint();
    }

    public static string TodoLane(JsonObject t)
    {
        if (t.Flag("done")) return "done";
        return NormalizeLane(t.Str("status"));
    }

    public static string NormalizeLane(string lane) => lane.Trim().ToLowerInvariant() switch
    {
        "doing" or "progress" or "inprogress" or "in progress" => "doing",
        "done" or "complete" or "completed" => "done",
        _ => "todo"
    };

    private ObservableCollection<TodoRow> LaneList(bool project, string lane) => (project, lane) switch
    {
        (true, "doing") => ProjectTodosDoing,
        (true, "done") => ProjectTodosDone,
        (true, _) => ProjectTodosBacklog,
        (false, "doing") => TodosDoing,
        (false, "done") => TodosDone,
        _ => TodosBacklog
    };

    public void SaveNote(JsonObject obj)
    {
        obj["updatedAt"] = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        Upsert("notes", obj);
        Paint();
    }

    public void SaveMinutes(JsonObject obj)
    {
        obj["updatedAt"] = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        Upsert("minutes", obj);
        Paint();
    }

    public async Task AttachPhotoNoteAsync(string localPath, string projectId)
    {
        if (string.IsNullOrWhiteSpace(projectId) || string.IsNullOrWhiteSpace(localPath)) return;
        var projName = BundleFactory.ProjectName(Bundle, projectId);
        var prevProject = SelectedFileProject;
        var prevFolder = SelectedFileFolder;
        SelectedFileProject = string.IsNullOrWhiteSpace(projName) ? SelectedFileProject : projName;
        SelectedFileFolder = "Photos";
        try
        {
            await SendFileAsync(localPath);
            var remote = $"{RemoteProjectDir()}/{Path.GetFileName(localPath)}";
            var now = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            SaveNote(new JsonObject
            {
                ["id"] = Guid.NewGuid().ToString(),
                ["projectId"] = projectId,
                ["title"] = "PHOTO  ·  " + DateTime.Now.ToString("d MMM yyyy  HH:mm", CultureInfo.InvariantCulture),
                ["body"] = "",
                ["photo"] = remote,
                ["createdAt"] = now,
                ["updatedAt"] = now
            });
        }
        finally
        {
            SelectedFileProject = prevProject;
            SelectedFileFolder = prevFolder;
        }
    }

    public void SaveMeasure(JsonObject obj)
    {
        obj["kind"] = MeasureMath.Kind(obj.Str("length"), obj.Str("breadth"), obj.Str("height"), true, "AREA");
        obj["autoKind"] = true;
        Upsert("measurements", obj);
        Paint();
    }
}
