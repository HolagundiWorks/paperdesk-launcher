using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;

namespace PaperDesk_Companion.Pages;

public sealed partial class HomePage : Page
{
    public HomePage()
    {
        InitializeComponent();
    }

    protected override void OnNavigatedTo(NavigationEventArgs e)
    {
        App.Session.Changed += Refresh;
        Refresh();
        _ = App.Session.RefreshConnectionAsync();
    }

    protected override void OnNavigatedFrom(NavigationEventArgs e) => App.Session.Changed -= Refresh;

    private void Refresh()
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            DashText.Text = App.Session.DashboardText;
            Subtitle.Text = App.Session.Connected ? "USB connected" : "Connect the phone over USB to sync";
        });
    }
}
