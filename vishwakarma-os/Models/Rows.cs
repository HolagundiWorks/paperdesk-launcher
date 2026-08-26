using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace PaperDesk_Companion.Models;

public sealed class ProjectRow
{
    public string Id { get; init; } = "";
    public string Name { get; init; } = "";
    public string Number { get; init; } = "";
    public string Updated { get; init; } = "";
    public int Measurements { get; init; }
    public int Notes { get; init; }
    public int Minutes { get; init; }
    public int Schedules { get; init; }
    public int Todos { get; init; }
    public string Counts => $"{Measurements} meas · {Notes} notes · {Minutes} min · {Schedules} sched · {Todos} todos";
}

public sealed class ScheduleRow
{
    public string Id { get; init; } = "";
    public string Time { get; init; } = "";
    public string Title { get; init; } = "";
    public string Project { get; init; } = "";
    public string Date { get; init; } = "";
    public string Notes { get; init; } = "";
    public bool Done { get; init; }
    public string DoneLabel => Done ? "Done" : "";
}

public sealed class TodoRow
{
    public string Id { get; init; } = "";
    public bool Done { get; init; }
    public string Lane { get; init; } = "todo";
    public string Title { get; init; } = "";
    public string Project { get; init; } = "";
    public string Due { get; init; } = "";
    public int Priority { get; init; }
    public string Notes { get; init; } = "";
    public string PriorityLabel => Priority switch { 0 => "Low", 2 => "High", _ => "Normal" };
    public string Meta
    {
        get
        {
            var parts = new List<string>();
            if (!string.IsNullOrWhiteSpace(Project)) parts.Add(Project);
            if (!string.IsNullOrWhiteSpace(Due)) parts.Add(Due);
            parts.Add(PriorityLabel);
            return string.Join("  ·  ", parts);
        }
    }
}

public sealed class NoteRow
{
    public string Id { get; init; } = "";
    public string Title { get; init; } = "";
    public string Project { get; init; } = "";
    public string Updated { get; init; } = "";
    public string Body { get; init; } = "";
    public string Photo { get; init; } = "";
    public string PhotoLabel => string.IsNullOrWhiteSpace(Photo) ? "" : "Photo";
}

public sealed class MinutesRow
{
    public string Id { get; init; } = "";
    public string Title { get; init; } = "";
    public string Project { get; init; } = "";
    public string Date { get; init; } = "";
    public string Attendees { get; init; } = "";
    public string Body { get; init; } = "";
    public string Photo { get; init; } = "";
    public string PhotoLabel => string.IsNullOrWhiteSpace(Photo) ? "" : "Photo";
    public string Meta
    {
        get
        {
            var parts = new List<string>();
            if (!string.IsNullOrWhiteSpace(Project)) parts.Add(Project);
            if (!string.IsNullOrWhiteSpace(Date)) parts.Add(Date);
            if (!string.IsNullOrWhiteSpace(Attendees)) parts.Add(Attendees);
            if (!string.IsNullOrWhiteSpace(PhotoLabel)) parts.Add(PhotoLabel);
            return string.Join("  ·  ", parts);
        }
    }
}

public sealed class MeasureRow
{
    public string Id { get; init; } = "";
    public int Item { get; init; }
    public string Project { get; init; } = "";
    public string Description { get; init; } = "";
    public string Length { get; init; } = "";
    public string Breadth { get; init; } = "";
    public string Height { get; init; } = "";
    public string Uom { get; init; } = "";
    public string Kind { get; init; } = "";
    public string Qty { get; init; } = "";
}

public sealed class AppRow
{
    public string Name { get; init; } = "";
    public string Package { get; init; } = "";
    public string Kind { get; init; } = "";
    public bool Enabled { get; init; }
    public string EnabledLabel => Enabled ? "On" : "Off";
}

public sealed class SettingFlag : INotifyPropertyChanged
{
    private bool _isOn;
    public string Key { get; }
    public string Label { get; }

    public SettingFlag(string key, string label, bool isOn)
    {
        Key = key;
        Label = label;
        _isOn = isOn;
    }

    public bool IsOn
    {
        get => _isOn;
        set
        {
            if (_isOn == value) return;
            _isOn = value;
            OnPropertyChanged();
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    private void OnPropertyChanged([CallerMemberName] string? name = null) =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
