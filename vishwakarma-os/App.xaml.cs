using Microsoft.UI.Xaml;
using PaperDesk_Companion.Services;

namespace PaperDesk_Companion;

public partial class App : Application
{
    public static MainWindow MainWindow { get; private set; } = null!;
    public static OsSession Session { get; } = new();

    public App()
    {
        UnhandledException += (_, e) =>
        {
            WriteCrash(e.Exception);
            e.Handled = true;
        };
        AppDomain.CurrentDomain.UnhandledException += (_, e) =>
        {
            if (e.ExceptionObject is Exception ex) WriteCrash(ex);
        };
        TaskScheduler.UnobservedTaskException += (_, e) =>
        {
            WriteCrash(e.Exception);
            e.SetObserved();
        };
        InitializeComponent();
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        try
        {
            MainWindow = new MainWindow();
            MainWindow.Activate();
            Session.Initialize();
        }
        catch (Exception ex)
        {
            WriteCrash(ex);
        }
    }

    private static void WriteCrash(Exception ex)
    {
        try
        {
            File.WriteAllText(Path.Combine(Paths.CacheDir(), "companion-crash.txt"), DateTime.Now + "\n" + ex);
        }
        catch
        {
            // ignore
        }
    }
}
