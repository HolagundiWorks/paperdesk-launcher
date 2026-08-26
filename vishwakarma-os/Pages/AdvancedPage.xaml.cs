using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Navigation;
using Windows.System;

namespace PaperDesk_Companion.Pages;

public sealed partial class AdvancedPage : Page
{
    public AdvancedPage() => InitializeComponent();

    protected override void OnNavigatedTo(NavigationEventArgs e)
    {
        App.Session.Changed += Refresh;
        Refresh();
    }

    protected override void OnNavigatedFrom(NavigationEventArgs e) => App.Session.Changed -= Refresh;

    private void Refresh() => DispatcherQueue.TryEnqueue(() => Log.Text = App.Session.AdvancedLog);

    private async void Run_Click(object sender, RoutedEventArgs e) => await RunAsync();

    private async void Cmd_KeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key == VirtualKey.Enter) await RunAsync();
    }

    private async Task RunAsync()
    {
        var cmd = Cmd.Text?.Trim();
        if (string.IsNullOrEmpty(cmd)) return;
        await App.Session.RunAdbAsync(cmd);
    }

    private async void Props_Click(object sender, RoutedEventArgs e) => await Preset("shell getprop");
    private async void Packages_Click(object sender, RoutedEventArgs e) => await Preset("shell pm list packages");
    private async void Logcat_Click(object sender, RoutedEventArgs e) => await Preset("logcat -d -t 80");
    private async void Df_Click(object sender, RoutedEventArgs e) => await Preset("shell df");

    private async Task Preset(string cmd)
    {
        Cmd.Text = cmd;
        await App.Session.RunAdbAsync(cmd);
    }
}
