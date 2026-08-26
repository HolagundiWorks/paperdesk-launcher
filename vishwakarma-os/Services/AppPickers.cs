using Windows.Storage.Pickers;
using WinRT.Interop;

namespace PaperDesk_Companion.Services;

public static class AppPickers
{
    private static nint Hwnd => WindowNative.GetWindowHandle(App.MainWindow);

    public static async Task<string?> OpenFileAsync(params string[] extensions)
    {
        var picker = new FileOpenPicker();
        InitializeWithWindow.Initialize(picker, Hwnd);
        picker.SuggestedStartLocation = PickerLocationId.DocumentsLibrary;
        if (extensions.Length == 0) picker.FileTypeFilter.Add("*");
        else
        {
            foreach (var ext in extensions)
                picker.FileTypeFilter.Add(ext.StartsWith('.') ? ext : "." + ext);
        }
        var file = await picker.PickSingleFileAsync();
        return file?.Path;
    }

    public static async Task<string?> SaveFileAsync(string suggestedName, string choiceName, string extension)
    {
        var picker = new FileSavePicker();
        InitializeWithWindow.Initialize(picker, Hwnd);
        picker.SuggestedStartLocation = PickerLocationId.DocumentsLibrary;
        picker.SuggestedFileName = suggestedName;
        picker.FileTypeChoices.Add(choiceName, [extension.StartsWith('.') ? extension : "." + extension]);
        var file = await picker.PickSaveFileAsync();
        return file?.Path;
    }

    public static async Task<string?> OpenFolderAsync()
    {
        var picker = new FolderPicker();
        InitializeWithWindow.Initialize(picker, Hwnd);
        picker.SuggestedStartLocation = PickerLocationId.DocumentsLibrary;
        picker.FileTypeFilter.Add("*");
        var folder = await picker.PickSingleFolderAsync();
        return folder?.Path;
    }
}
