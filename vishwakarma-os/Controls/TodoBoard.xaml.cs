using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using PaperDesk_Companion.Dialogs;
using PaperDesk_Companion.Models;
using PaperDesk_Companion.Services;
using Windows.ApplicationModel.DataTransfer;

namespace PaperDesk_Companion.Controls;

public sealed partial class TodoBoard : UserControl
{
    public static readonly DependencyProperty ProjectScopedProperty =
        DependencyProperty.Register(nameof(ProjectScoped), typeof(bool), typeof(TodoBoard),
            new PropertyMetadata(false, (_, _) => { }));

    public bool ProjectScoped
    {
        get => (bool)GetValue(ProjectScopedProperty);
        set => SetValue(ProjectScopedProperty, value);
    }

    public TodoRow? Selected { get; private set; }

    public TodoBoard()
    {
        InitializeComponent();
        Loaded += OnLoaded;
        Unloaded += OnUnloaded;
    }

    private OsSession Session => App.Session;

    private void OnLoaded(object sender, RoutedEventArgs e)
    {
        BindLists();
        Session.Changed += RefreshHeaders;
        RefreshHeaders();
    }

    private void OnUnloaded(object sender, RoutedEventArgs e) => Session.Changed -= RefreshHeaders;

    private void BindLists()
    {
        BacklogList.ItemsSource = ProjectScoped ? Session.ProjectTodosBacklog : Session.TodosBacklog;
        DoingList.ItemsSource = ProjectScoped ? Session.ProjectTodosDoing : Session.TodosDoing;
        DoneList.ItemsSource = ProjectScoped ? Session.ProjectTodosDone : Session.TodosDone;
    }

    private void RefreshHeaders()
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            BindLists();
            var backlog = (ProjectScoped ? Session.ProjectTodosBacklog : Session.TodosBacklog).Count;
            var doing = (ProjectScoped ? Session.ProjectTodosDoing : Session.TodosDoing).Count;
            var done = (ProjectScoped ? Session.ProjectTodosDone : Session.TodosDone).Count;
            BacklogHeader.Text = $"To do  ({backlog})";
            DoingHeader.Text = $"In progress  ({doing})";
            DoneHeader.Text = $"Done  ({done})";
        });
    }

    private async void AddBacklog_Click(object sender, RoutedEventArgs e) =>
        await EditorDialogs.EditTodoAsync(XamlRoot, Session, null, ProjectScoped ? Session.SelectedProjectId : null);

    public async Task AddAsync() =>
        await EditorDialogs.EditTodoAsync(XamlRoot, Session, null, ProjectScoped ? Session.SelectedProjectId : null);

    public async Task EditSelectedAsync()
    {
        if (Selected is null) return;
        await EditorDialogs.EditTodoAsync(XamlRoot, Session, Selected.Id, ProjectScoped ? Session.SelectedProjectId : null);
    }

    public void DeleteSelected()
    {
        if (Selected is null) return;
        Session.DeleteFrom("todos", Selected.Id);
        Selected = null;
    }

    private void List_ItemClick(object sender, ItemClickEventArgs e)
    {
        if (e.ClickedItem is TodoRow row) Selected = row;
        if (sender is ListView list)
        {
            foreach (var other in new[] { BacklogList, DoingList, DoneList })
            {
                if (other != list) other.SelectedItem = null;
            }
        }
    }

    private async void List_DoubleTapped(object sender, DoubleTappedRoutedEventArgs e) =>
        await EditSelectedAsync();

    private void List_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (sender is ListView list && list.SelectedItem is TodoRow row)
            Selected = row;
    }

    private void List_DragItemsStarting(object sender, DragItemsStartingEventArgs e)
    {
        if (e.Items.FirstOrDefault() is TodoRow row)
        {
            e.Data.SetText(row.Id);
            e.Data.RequestedOperation = DataPackageOperation.Move;
        }
    }

    private void List_DragOver(object sender, DragEventArgs e)
    {
        e.AcceptedOperation = DataPackageOperation.Move;
        e.DragUIOverride.Caption = "Move card";
    }

    private async void List_Drop(object sender, DragEventArgs e)
    {
        var deferral = e.GetDeferral();
        try
        {
            if (sender is not FrameworkElement fe) return;
            if (!e.DataView.Contains(StandardDataFormats.Text)) return;
            var id = await e.DataView.GetTextAsync();
            if (string.IsNullOrWhiteSpace(id)) return;
            Session.SetTodoStatus(id, fe.Tag as string ?? "todo");
        }
        finally
        {
            deferral.Complete();
        }
    }
}
