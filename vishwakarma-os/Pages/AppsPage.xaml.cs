using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using PaperDesk_Companion.Models;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion.Pages;

public sealed partial class AppsPage : Page
{
    public OsSession Session => App.Session;
    public AppsPage() => InitializeComponent();

    protected override void OnNavigatedTo(NavigationEventArgs e) => _ = Session.LoadAppsAsync();

    private AppRow? Selected => List.SelectedItem as AppRow;

    private async void Enable_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        await Session.PkgActionAsync("enable", Selected);
    }
    private async void Disable_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        await Session.PkgActionAsync("disable", Selected);
    }
    private async void Open_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        await Session.PkgActionAsync("open", Selected);
    }
    private async void Clear_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        await Session.PkgActionAsync("clear", Selected);
    }
    private async void Uninstall_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        await Session.PkgActionAsync("uninstall", Selected);
    }
    private async void Refresh_Click(object sender, RoutedEventArgs e) => await Session.LoadAppsAsync();
}
