using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PaperDesk_Companion.Dialogs;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion.Pages;

public sealed partial class DevicePage : Page
{
    public DevicePage() => InitializeComponent();

    private async void Sync_Click(object sender, RoutedEventArgs e) => await App.Session.SyncNowAsync();

    private async void Backup_Click(object sender, RoutedEventArgs e)
    {
        var path = await AppPickers.SaveFileAsync("projectos-backup-" + JsonUtil.Stamp(), "Zip archive", ".zip");
        if (path is null) return;
        await App.Session.BackupAsync(path);
    }

    private async void Restore_Click(object sender, RoutedEventArgs e)
    {
        var path = await AppPickers.OpenFileAsync(".zip", ".json");
        if (path is null) return;
        await App.Session.RestoreAsync(path, "full");
    }

    private async void Install_Click(object sender, RoutedEventArgs e)
    {
        var path = await AppPickers.OpenFileAsync(".apk");
        if (path is null) return;
        await App.Session.InstallApkAsync(path);
    }

    private async void Reboot_Click(object sender, RoutedEventArgs e)
    {
        if (!await EditorDialogs.ConfirmAsync(XamlRoot, "Restart the phone?")) return;
        await App.Session.RunIoAsync("Restarting...", () => { AdbClient.Run("reboot"); return "OK"; });
    }

    private async void Recovery_Click(object sender, RoutedEventArgs e)
    {
        if (!await EditorDialogs.ConfirmAsync(XamlRoot, "Reboot into recovery?")) return;
        await App.Session.RunIoAsync("Rebooting recovery...", () => { AdbClient.Run("reboot", "recovery"); return "OK"; });
    }
}
