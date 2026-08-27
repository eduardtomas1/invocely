package ui;

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class KeyboardShortcutsTest {
    @Test
    void menuShortcutsUseThePlatformModifier() {
        KeyStroke shortcut = KeyboardShortcuts.menu(KeyEvent.VK_S);

        assertEquals(KeyEvent.VK_S, shortcut.getKeyCode());
        assertNotEquals(0, shortcut.getModifiers());
    }
}
