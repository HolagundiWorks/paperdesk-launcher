using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PaperDesk_Companion.Dialogs;
using PaperDesk_Companion.Models;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion.Pages;

public sealed partial class NotesPage : Page
{
    public OsSession Session => App.Session;
    public NotesPage() => InitializeComponent();
    private NoteRow? Selected => List.SelectedItem as NoteRow;
    private async void New_Click(object sender, RoutedEventArgs e) => await EditorDialogs.EditNoteAsync(XamlRoot, Session, null);
    private async void Edit_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        await EditorDialogs.EditNoteAsync(XamlRoot, Session, Selected.Id);
    }
    private void Delete_Click(object sender, RoutedEventArgs e)
    {
        if (Selected is null) return;
        Session.DeleteFrom("notes", Selected.Id);
    }
}
