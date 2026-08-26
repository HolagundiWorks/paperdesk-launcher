using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Navigation;
using PaperDesk_Companion.Dialogs;
using PaperDesk_Companion.Models;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion.Pages;

public sealed partial class ProjectsPage : Page
{
    public OsSession Session => App.Session;

    public ProjectsPage()
    {
        InitializeComponent();
    }

    protected override void OnNavigatedTo(NavigationEventArgs e)
    {
        Session.Changed += OnChanged;
        ShowListOrDetail();
        OnChanged();
    }

    protected override void OnNavigatedFrom(NavigationEventArgs e) => Session.Changed -= OnChanged;

    private void OnChanged()
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            DetailTitle.Text = Session.SelectedProjectName;
            MeasTotal.Text = Session.MeasureTotal;
            ShowListOrDetail();
        });
    }

    private void ShowListOrDetail()
    {
        var detail = !string.IsNullOrWhiteSpace(Session.SelectedProjectId);
        ListState.Visibility = detail ? Visibility.Collapsed : Visibility.Visible;
        DetailState.Visibility = detail ? Visibility.Visible : Visibility.Collapsed;
    }

    private ProjectRow? SelectedProject => ProjectList.SelectedItem as ProjectRow;
    private MeasureRow? SelectedMeas => MeasList.SelectedItem as MeasureRow;
    private NoteRow? SelectedPNote => PNoteList.SelectedItem as NoteRow;
    private MinutesRow? SelectedPMin => PMinList.SelectedItem as MinutesRow;
    private ScheduleRow? SelectedPSched => PSchedList.SelectedItem as ScheduleRow;

    private void OpenSelected()
    {
        if (SelectedProject is null) return;
        Session.SelectedProjectId = SelectedProject.Id;
        Session.Paint();
        DetailPivot.SelectedIndex = 0;
    }

    private void ProjectList_ItemClick(object sender, ItemClickEventArgs e) =>
        ProjectList.SelectedItem = e.ClickedItem;

    private void ProjectList_DoubleTapped(object sender, DoubleTappedRoutedEventArgs e) => OpenSelected();

    private async void New_Click(object sender, RoutedEventArgs e) => await EditorDialogs.EditProjectAsync(XamlRoot, Session, null);
    private async void Edit_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedProject is null) return;
        await EditorDialogs.EditProjectAsync(XamlRoot, Session, SelectedProject.Id);
    }
    private void Open_Click(object sender, RoutedEventArgs e) => OpenSelected();
    private async void Delete_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedProject is null) return;
        if (!await EditorDialogs.ConfirmAsync(XamlRoot, "Delete this project and its measurements, notes, minutes, schedules, and todos?")) return;
        Session.DeleteProject(SelectedProject.Id);
    }

    private void Back_Click(object sender, RoutedEventArgs e)
    {
        Session.SelectedProjectId = null;
        Session.Paint();
    }

    private async void MeasNew_Click(object sender, RoutedEventArgs e) => await EditorDialogs.EditMeasureAsync(XamlRoot, Session, null);
    private async void MeasEdit_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedMeas is null) return;
        await EditorDialogs.EditMeasureAsync(XamlRoot, Session, SelectedMeas.Id);
    }
    private void MeasDelete_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedMeas is null) return;
        Session.DeleteFrom("measurements", SelectedMeas.Id);
    }

    private async void PNoteNew_Click(object sender, RoutedEventArgs e) => await EditorDialogs.EditNoteAsync(XamlRoot, Session, null, Session.SelectedProjectId);
    private async void PNotePhoto_Click(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(Session.SelectedProjectId)) return;
        var path = await AppPickers.OpenFileAsync(".jpg", ".jpeg", ".png");
        if (path is null) return;
        await Session.AttachPhotoNoteAsync(path, Session.SelectedProjectId);
    }
    private async void PNoteEdit_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedPNote is null) return;
        await EditorDialogs.EditNoteAsync(XamlRoot, Session, SelectedPNote.Id, Session.SelectedProjectId);
    }
    private void PNoteDelete_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedPNote is null) return;
        Session.DeleteFrom("notes", SelectedPNote.Id);
    }

    private async void PMinNew_Click(object sender, RoutedEventArgs e) => await EditorDialogs.EditMinutesAsync(XamlRoot, Session, null, Session.SelectedProjectId);
    private async void PMinEdit_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedPMin is null) return;
        await EditorDialogs.EditMinutesAsync(XamlRoot, Session, SelectedPMin.Id, Session.SelectedProjectId);
    }
    private void PMinDelete_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedPMin is null) return;
        Session.DeleteFrom("minutes", SelectedPMin.Id);
    }

    private async void PSchedNew_Click(object sender, RoutedEventArgs e) => await EditorDialogs.EditScheduleAsync(XamlRoot, Session, null, Session.SelectedProjectId);
    private async void PSchedEdit_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedPSched is null) return;
        await EditorDialogs.EditScheduleAsync(XamlRoot, Session, SelectedPSched.Id, Session.SelectedProjectId);
    }
    private void PSchedDelete_Click(object sender, RoutedEventArgs e)
    {
        if (SelectedPSched is null) return;
        Session.DeleteFrom("schedules", SelectedPSched.Id);
    }

    private async void PTodoNew_Click(object sender, RoutedEventArgs e) => await PTodoBoard.AddAsync();
    private async void PTodoEdit_Click(object sender, RoutedEventArgs e) => await PTodoBoard.EditSelectedAsync();
    private void PTodoDelete_Click(object sender, RoutedEventArgs e) => PTodoBoard.DeleteSelected();
}
