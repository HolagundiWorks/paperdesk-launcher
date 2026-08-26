package com.paperdesk.companion

import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    System.setProperty("awt.useSystemAAFontSettings", "on")
    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    SwingUtilities.invokeLater { CompanionFrame().isVisible = true }
}
