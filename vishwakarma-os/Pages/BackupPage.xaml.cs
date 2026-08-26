using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion.Pages;

public sealed partial class BackupPage : Page
{
    public OsSession Session => App.Session;
    public BackupPage() => InitializeComponent();

    private async void Backup_Click(object sender, RoutedEventArgs e)
    {
        var path = await AppPickers.SaveFileAsync("projectos-backup-" + JsonUtil.Stamp(), "Zip archive", ".zip");
        if (path is null) return;
        await Session.BackupAsync(path);
    }

    private async void Data_Click(object sender, RoutedEventArgs e) => await Restore("data");
    private async void Config_Click(object sender, RoutedEventArgs e) => await Restore("config");
    private async void Full_Click(object sender, RoutedEventArgs e) => await Restore("full");

    private async Task Restore(string mode)
    {
        var path = await AppPickers.OpenFileAsync(".zip", ".json");
        if (path is null) return;
        await Session.RestoreAsync(path, mode);
    }
}
