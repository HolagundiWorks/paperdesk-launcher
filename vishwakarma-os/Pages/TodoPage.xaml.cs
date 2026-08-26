using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace PaperDesk_Companion.Pages;

public sealed partial class TodoPage : Page
{
    public TodoPage() => InitializeComponent();

    private async void New_Click(object sender, RoutedEventArgs e) => await Board.AddAsync();
    private async void Edit_Click(object sender, RoutedEventArgs e) => await Board.EditSelectedAsync();
    private void Delete_Click(object sender, RoutedEventArgs e) => Board.DeleteSelected();
}
