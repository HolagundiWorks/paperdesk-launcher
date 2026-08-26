using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace PaperDesk_Companion.Pages;

public sealed partial class SyncPage : Page
{
    public SyncPage() => InitializeComponent();
    private async void Sync_Click(object sender, RoutedEventArgs e) => await App.Session.SyncNowAsync();
    private async void Push_Click(object sender, RoutedEventArgs e) => await App.Session.PushAsync();
}
