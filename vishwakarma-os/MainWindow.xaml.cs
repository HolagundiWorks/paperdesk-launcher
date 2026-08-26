using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PaperDesk_Companion.Pages;
using PaperDesk_Companion.Services;
using Windows.Graphics;

namespace PaperDesk_Companion;

public sealed partial class MainWindow : Window
{
    public MainWindow()
    {
        try
        {
            InitializeComponent();
            ExtendsContentIntoTitleBar = true;
            SetTitleBar(AppTitleBar);
            AppWindow.TitleBar.PreferredHeightOption = TitleBarHeightOption.Tall;
            var icon = Path.Combine(AppContext.BaseDirectory, "Assets", "AppIcon.ico");
            if (File.Exists(icon)) AppWindow.SetIcon(icon);
            AppWindow.Resize(new SizeInt32(1280, 800));
            App.Session.Changed += OnSessionChanged;
            Closed += (_, _) => App.Session.Changed -= OnSessionChanged;
            OnSessionChanged();
        }
        catch (Exception ex)
        {
            File.WriteAllText(
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".paperdesk-companion", "companion-crash.txt"),
                DateTime.Now + "\nMainWindow\n" + ex);
            throw;
        }
    }

    private void OnSessionChanged()
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            StatusInfo.Title = App.Session.ConnectionTitle;
            StatusInfo.Message = App.Session.Status;
            StatusInfo.Severity = App.Session.Connected ? InfoBarSeverity.Success : InfoBarSeverity.Informational;
            StatusInfo.IsOpen = true;
        });
    }

    private void TitleBar_PaneToggleRequested(TitleBar sender, object args) =>
        NavView.IsPaneOpen = !NavView.IsPaneOpen;

    private void TitleBar_BackRequested(TitleBar sender, object args)
    {
        if (NavFrame.CanGoBack) NavFrame.GoBack();
    }

    private void SearchBox_QuerySubmitted(AutoSuggestBox sender, AutoSuggestBoxQuerySubmittedEventArgs args)
    {
        App.Session.Search = sender.Text ?? "";
        App.Session.Paint();
    }

    private void SearchBox_TextChanged(AutoSuggestBox sender, AutoSuggestBoxTextChangedEventArgs args)
    {
        if (args.Reason != AutoSuggestionBoxTextChangeReason.UserInput) return;
        App.Session.Search = sender.Text ?? "";
        App.Session.Paint();
    }

    private async void Sync_Click(object sender, RoutedEventArgs e) => await App.Session.SyncNowAsync();
    private async void Push_Click(object sender, RoutedEventArgs e) => await App.Session.PushAsync();

    private void NavView_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        if (args.IsSettingsSelected)
        {
            NavFrame.Navigate(typeof(SettingsPage));
            return;
        }
        if (args.SelectedItem is not NavigationViewItem item) return;
        var page = item.Tag switch
        {
            "home" => typeof(HomePage),
            "projects" => typeof(ProjectsPage),
            "schedule" => typeof(SchedulePage),
            "todo" => typeof(TodoPage),
            "notes" => typeof(NotesPage),
            "minutes" => typeof(MinutesPage),
            "files" => typeof(FilesPage),
            "apps" => typeof(AppsPage),
            "device" => typeof(DevicePage),
            "backup" => typeof(BackupPage),
            "sync" => typeof(SyncPage),
            "advanced" => typeof(AdvancedPage),
            _ => typeof(HomePage)
        };
        NavFrame.Navigate(page);
    }
}
