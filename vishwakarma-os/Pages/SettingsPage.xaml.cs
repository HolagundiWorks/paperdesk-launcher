using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using PaperDesk_Companion.Dialogs;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion.Pages;

public sealed partial class SettingsPage : Page
{
    public OsSession Session => App.Session;

    public SettingsPage() => InitializeComponent();

    protected override void OnNavigatedTo(NavigationEventArgs e)
    {
        Session.Changed += Refresh;
        Refresh();
    }

    protected override void OnNavigatedFrom(NavigationEventArgs e)
    {
        Collect();
        Session.Changed -= Refresh;
    }

    private void Refresh()
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            PagesBox.Text = Session.PagesText;
            JsonBox.Text = Session.SettingsJson;
        });
    }

    private void Collect()
    {
        Session.PagesText = PagesBox.Text ?? "";
        Session.SettingsJson = JsonBox.Text ?? "{}";
        Session.CollectSettings();
    }

    private async void Apply_Click(object sender, RoutedEventArgs e)
    {
        Collect();
        await Session.ApplySettingsAsync();
    }

    private void Profile_Click(object sender, RoutedEventArgs e)
    {
        Session.LoadArchitectureProfile();
        Refresh();
    }

    private async void SaveProfile_Click(object sender, RoutedEventArgs e)
    {
        Collect();
        var name = await EditorDialogs.PromptAsync(XamlRoot, "Save profile", "Profile name", "Acharya Mode");
        if (string.IsNullOrWhiteSpace(name)) return;
        Session.SaveProfile(name);
    }
}
