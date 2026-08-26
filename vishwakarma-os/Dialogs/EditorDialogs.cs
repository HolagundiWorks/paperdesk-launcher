using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PaperDesk_Companion.Services;
using System.Text.Json.Nodes;

namespace PaperDesk_Companion.Dialogs;

public static class EditorDialogs
{
    public static async Task<bool> ConfirmAsync(XamlRoot root, string message, string title = "Confirm")
    {
        var dialog = new ContentDialog
        {
            XamlRoot = root,
            Title = title,
            Content = message,
            PrimaryButtonText = "OK",
            CloseButtonText = "Cancel",
            DefaultButton = ContentDialogButton.Primary
        };
        return await dialog.ShowAsync() == ContentDialogResult.Primary;
    }

    public static async Task AlertAsync(XamlRoot root, string message, string title = "Acharya OS")
    {
        var dialog = new ContentDialog
        {
            XamlRoot = root,
            Title = title,
            Content = message,
            CloseButtonText = "Close",
            DefaultButton = ContentDialogButton.Close
        };
        await dialog.ShowAsync();
    }

    public static async Task<string?> PromptAsync(XamlRoot root, string title, string label, string value = "")
    {
        var box = new TextBox { Text = value, Header = label, MinWidth = 320 };
        var dialog = new ContentDialog
        {
            XamlRoot = root,
            Title = title,
            Content = box,
            PrimaryButtonText = "OK",
            CloseButtonText = "Cancel",
            DefaultButton = ContentDialogButton.Primary
        };
        return await dialog.ShowAsync() == ContentDialogResult.Primary ? box.Text : null;
    }

    public static async Task EditProjectAsync(XamlRoot root, OsSession session, string? id)
    {
        var existing = id is null ? null : session.Find("projects", id);
        var name = Field("Name", existing?.Str("name") ?? "");
        var number = Field("Number", existing?.Str("number") ?? "");
        if (!await ShowForm(root, id is null ? "New project" : "Edit project", name, number)) return;
        var obj = existing ?? NewId();
        obj["name"] = name.Text.Trim();
        obj["number"] = number.Text.Trim();
        session.SaveProject(obj);
    }

    public static async Task EditScheduleAsync(XamlRoot root, OsSession session, string? id, string? forceProjectId = null)
    {
        var existing = id is null ? null : session.Find("schedules", id);
        var preset = forceProjectId ?? existing?.Str("projectId") ?? "";
        var title = Field("Title", existing?.Str("title") ?? "");
        var project = ProjectCombo(session, BundleFactory.ProjectName(session.Bundle, preset));
        var date = new DatePicker { Header = "Date", Date = FromMs(existing?.Long("dayStart") ?? DateTimeOffset.Now.ToUnixTimeMilliseconds()) };
        var time = new TimePicker { Header = "Time", ClockIdentifier = "24HourClock", Time = new TimeSpan(existing?.Int("hour") ?? 9, existing?.Int("minute") ?? 0, 0) };
        var notes = Field("Notes", existing?.Str("notes") ?? "");
        var done = new ToggleSwitch { Header = "Done", IsOn = existing?.Flag("done") == true };
        if (!await ShowForm(root, id is null ? "New schedule" : "Edit schedule", title, project, date, time, notes, done)) return;
        var obj = existing ?? NewId();
        obj["title"] = title.Text.Trim();
        obj["projectId"] = session.IdForName(project.SelectedItem as string ?? "");
        obj["dayStart"] = new DateTimeOffset(date.Date.Date, TimeSpan.Zero).ToUnixTimeMilliseconds();
        obj["hour"] = Math.Clamp(time.Time.Hours, 0, 23);
        obj["minute"] = Math.Clamp(time.Time.Minutes, 0, 59);
        obj["notes"] = notes.Text;
        obj["done"] = done.IsOn;
        session.SaveSchedule(obj);
    }

    public static async Task EditTodoAsync(XamlRoot root, OsSession session, string? id, string? forceProjectId = null)
    {
        var existing = id is null ? null : session.Find("todos", id);
        var preset = forceProjectId ?? existing?.Str("projectId") ?? "";
        var title = Field("Title", existing?.Str("text") ?? "");
        var projectName = BundleFactory.ProjectName(session.Bundle, preset);
        if (string.IsNullOrWhiteSpace(projectName)) projectName = existing?.Str("project") ?? "";
        var project = ProjectCombo(session, projectName);
        var due = new DatePicker { Header = "Due", Date = FromMs(existing?.Long("dueAt") ?? DateTimeOffset.Now.ToUnixTimeMilliseconds()) };
        var priority = new ComboBox { Header = "Priority", HorizontalAlignment = HorizontalAlignment.Stretch };
        priority.Items.Add("Low");
        priority.Items.Add("Normal");
        priority.Items.Add("High");
        priority.SelectedIndex = Math.Clamp(existing?.Int("priority", 1) ?? 1, 0, 2);
        var notes = Field("Notes", existing?.Str("notes") ?? "");
        var status = new ComboBox { Header = "Board column", HorizontalAlignment = HorizontalAlignment.Stretch };
        status.Items.Add("To do");
        status.Items.Add("In progress");
        status.Items.Add("Done");
        status.SelectedIndex = OsSession.TodoLane(existing ?? new JsonObject()) switch
        {
            "doing" => 1,
            "done" => 2,
            _ => 0
        };
        if (!await ShowForm(root, id is null ? "New todo" : "Edit todo", title, project, due, priority, notes, status)) return;
        var obj = existing ?? NewId();
        var pname = project.SelectedItem as string ?? "";
        obj["text"] = title.Text.Trim();
        obj["projectId"] = session.IdForName(pname);
        obj["project"] = pname;
        obj["dueAt"] = new DateTimeOffset(due.Date.Date, TimeSpan.Zero).ToUnixTimeMilliseconds();
        obj["priority"] = priority.SelectedIndex;
        obj["notes"] = notes.Text;
        obj["status"] = status.SelectedIndex switch { 1 => "doing", 2 => "done", _ => "todo" };
        obj["done"] = status.SelectedIndex == 2;
        session.SaveTodo(obj);
    }

    public static async Task EditNoteAsync(XamlRoot root, OsSession session, string? id, string? forceProjectId = null)
    {
        var existing = id is null ? null : session.Find("notes", id);
        var preset = forceProjectId ?? existing?.Str("projectId") ?? "";
        var title = Field("Title", existing?.Str("title") ?? "");
        var project = ProjectCombo(session, BundleFactory.ProjectName(session.Bundle, preset));
        var body = new TextBox
        {
            Header = "Body",
            Text = existing?.Str("body") ?? "",
            AcceptsReturn = true,
            TextWrapping = TextWrapping.Wrap,
            MinHeight = 160,
            MinWidth = 420
        };
        if (!await ShowForm(root, id is null ? "New note" : "Edit note", title, project, body)) return;
        var obj = existing ?? NewId();
        if (existing is null) obj["createdAt"] = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        obj["title"] = title.Text.Trim();
        obj["projectId"] = session.IdForName(project.SelectedItem as string ?? "");
        obj["body"] = body.Text;
        if (existing is not null) obj["photo"] = existing.Str("photo");
        session.SaveNote(obj);
    }

    public static async Task EditMinutesAsync(XamlRoot root, OsSession session, string? id, string? forceProjectId = null)
    {
        var existing = id is null ? null : session.Find("minutes", id);
        var preset = forceProjectId ?? existing?.Str("projectId") ?? "";
        var title = Field("Title", existing?.Str("title") ?? "");
        var project = ProjectCombo(session, BundleFactory.ProjectName(session.Bundle, preset));
        var date = new DatePicker { Header = "Date", Date = FromMs(existing?.Long("dayStart") ?? DateTimeOffset.Now.ToUnixTimeMilliseconds()) };
        var attendees = Field("Attendees", existing?.Str("attendees") ?? "");
        var body = new TextBox
        {
            Header = "Minutes",
            Text = existing?.Str("body") ?? "",
            AcceptsReturn = true,
            TextWrapping = TextWrapping.Wrap,
            MinHeight = 140,
            MinWidth = 420
        };
        var photo = Field("Photo path", existing?.Str("photo") ?? "");
        if (!await ShowForm(root, id is null ? "New minutes" : "Edit minutes", title, project, date, attendees, body, photo)) return;
        var obj = existing ?? NewId();
        if (existing is null) obj["createdAt"] = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        var label = title.Text.Trim();
        if (string.IsNullOrWhiteSpace(label))
            label = "MINUTES  ·  " + date.Date.ToString("d MMM yyyy", System.Globalization.CultureInfo.InvariantCulture);
        obj["title"] = label;
        obj["projectId"] = session.IdForName(project.SelectedItem as string ?? "");
        obj["dayStart"] = new DateTimeOffset(date.Date.Date, TimeSpan.Zero).ToUnixTimeMilliseconds();
        obj["attendees"] = attendees.Text.Trim();
        obj["body"] = body.Text;
        obj["photo"] = photo.Text.Trim();
        session.SaveMinutes(obj);
    }

    public static async Task EditMeasureAsync(XamlRoot root, OsSession session, string? id)
    {
        var pid = session.SelectedProjectId
            ?? (id is null ? null : session.Find("measurements", id)?.Str("projectId"));
        if (string.IsNullOrWhiteSpace(pid))
        {
            await AlertAsync(root, "Open a project to use Measurement Book.");
            return;
        }
        var existing = id is null ? null : session.Find("measurements", id);
        var desc = Field("Description", existing?.Str("description") ?? "");
        var length = Field("Length", existing?.Str("length") ?? "");
        var breadth = Field("Breadth", existing?.Str("breadth") ?? "");
        var height = Field("Height", existing?.Str("height") ?? "");
        var uom = new ComboBox { Header = "Unit", HorizontalAlignment = HorizontalAlignment.Stretch };
        foreach (var u in new[] { "M", "MM", "CM", "FT", "IN" }) uom.Items.Add(u);
        var current = (existing?.Str("uom", "M") ?? "M").ToUpperInvariant();
        uom.SelectedItem = uom.Items.Cast<string>().Contains(current) ? current : "M";
        if (!await ShowForm(root, id is null ? "New measurement" : "Edit measurement", desc, length, breadth, height, uom)) return;
        var obj = existing ?? NewId();
        obj["projectId"] = pid;
        obj["description"] = desc.Text.Trim();
        obj["length"] = length.Text.Trim();
        obj["breadth"] = breadth.Text.Trim();
        obj["height"] = height.Text.Trim();
        obj["uom"] = (uom.SelectedItem as string ?? "M").ToUpperInvariant();
        session.SaveMeasure(obj);
    }

    private static JsonObject NewId() => new() { ["id"] = Guid.NewGuid().ToString() };

    private static TextBox Field(string header, string value) => new()
    {
        Header = header,
        Text = value,
        MinWidth = 360
    };

    private static ComboBox ProjectCombo(OsSession session, string selected)
    {
        var box = new ComboBox { Header = "Project", HorizontalAlignment = HorizontalAlignment.Stretch, MinWidth = 360 };
        box.Items.Add("");
        foreach (var name in session.ProjectNames.Where(n => n != "Project" || session.Projects.Count == 0))
            box.Items.Add(name);
        if (!string.IsNullOrWhiteSpace(selected) && !box.Items.Contains(selected)) box.Items.Add(selected);
        box.SelectedItem = string.IsNullOrWhiteSpace(selected) ? "" : selected;
        return box;
    }

    private static DateTimeOffset FromMs(long ms)
    {
        if (ms <= 0) return DateTimeOffset.Now;
        return DateTimeOffset.FromUnixTimeMilliseconds(ms).ToLocalTime();
    }

    private static async Task<bool> ShowForm(XamlRoot root, string title, params UIElement[] fields)
    {
        var panel = new StackPanel { Spacing = 12, MinWidth = 360 };
        foreach (var f in fields) panel.Children.Add(f);
        var dialog = new ContentDialog
        {
            XamlRoot = root,
            Title = title,
            Content = new ScrollViewer { Content = panel, MaxHeight = 520 },
            PrimaryButtonText = "Save",
            CloseButtonText = "Cancel",
            DefaultButton = ContentDialogButton.Primary
        };
        return await dialog.ShowAsync() == ContentDialogResult.Primary;
    }
}
