using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using PaperDesk_Companion.Dialogs;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion.Pages;

public sealed partial class FilesPage : Page
{
    public OsSession Session => App.Session;
    private bool _ready;

    public FilesPage() => InitializeComponent();

    protected override void OnNavigatedTo(NavigationEventArgs e)
    {
        Session.Changed += Refresh;
        ProjectBox.SelectedItem = Session.SelectedFileProject;
        FolderBox.SelectedItem = Session.SelectedFileFolder;
        _ready = true;
        Refresh();
        _ = Session.LoadFilesAsync();
    }

    protected override void OnNavigatedFrom(NavigationEventArgs e)
    {
        _ready = false;
        Session.Changed -= Refresh;
    }

    private void Refresh() => DispatcherQueue.TryEnqueue(() => Listing.Text = Session.FileListing);

    private void Filter_Changed(object sender, SelectionChangedEventArgs e)
    {
        if (!_ready) return;
        Session.SelectedFileProject = ProjectBox.SelectedItem as string ?? Session.SelectedFileProject;
        Session.SelectedFileFolder = FolderBox.SelectedItem as string ?? Session.SelectedFileFolder;
        _ = Session.LoadFilesAsync();
    }

    private async void Send_Click(object sender, RoutedEventArgs e)
    {
        var path = await AppPickers.OpenFileAsync("*");
        if (path is null) return;
        await Session.SendFileAsync(path);
    }

    private async void Recv_Click(object sender, RoutedEventArgs e)
    {
        var remote = await EditorDialogs.PromptAsync(XamlRoot, "Phone to PC", "Remote path", Session.RemoteProjectDir() + "/");
        if (string.IsNullOrWhiteSpace(remote)) return;
        var dir = await AppPickers.OpenFolderAsync();
        if (dir is null) return;
        await Session.RecvFileAsync(remote, dir);
    }

    private async void Refresh_Click(object sender, RoutedEventArgs e) => await Session.LoadFilesAsync();
}
