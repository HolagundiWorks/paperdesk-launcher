using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PaperDesk_Companion.Dialogs;
using PaperDesk_Companion.Models;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion.Pages;

public sealed partial class SchedulePage : Page
{
    public OsSession Session => App.Session;
    public SchedulePage() => InitializeComponent();
    private ScheduleRow? Selected => List.SelectedItem as ScheduleRow;
    private async void New_Click(object sender, RoutedEventArgs e) => await EditorDialogs.EditScheduleAsync(XamlRoot, Session, null);
    private async void Edit_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        await EditorDialogs.EditScheduleAsync(XamlRoot, Session, Selected.Id);
    }
    private void Delete_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        Session.DeleteFrom("schedules", Selected.Id);
    }
}
