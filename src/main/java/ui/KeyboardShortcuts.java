package ui;

import javax.swing.KeyStroke;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;

/** Platform-native menu shortcuts with a safe fallback for headless tests. */
public final class KeyboardShortcuts {
    private KeyboardShortcuts() { }

    public static KeyStroke menu(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, menuMask());
    }

    static int menuMask() {
        try {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (HeadlessException ex) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }
}
