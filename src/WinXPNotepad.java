import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class WinXPNotepad extends JFrame {

    private final JTextArea textArea;
    private final JLabel statusLabel;

    // Zmienne do przechowywania informacji o aktualnie otwartym pliku
    private File currentFile = null;
    private Charset currentCharset = StandardCharsets.UTF_8;

    // Kolory i czcionki w stylu Windows XP
    private final Color xpBackground = new Color(236, 233, 216); // Klasyczny beżowy kolor XP
    private final Font xpUiFont = new Font("Tahoma", Font.PLAIN, 12); // Domyślna czcionka interfejsu XP
    private final Font xpTextFont = new Font("Courier New", Font.PLAIN, 14); // Klasyczna czcionka notatnika

    public WinXPNotepad() {
        setTitle("Notatnik (Styl Windows XP) - Brak tytułu");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(xpBackground);

        // --- PASEK NARZĘDZI (TOOLBAR) ---
        JToolBar toolBar = new JToolBar();
        toolBar.setBackground(xpBackground);
        toolBar.setFloatable(false); // Blokada odpinania paska
        toolBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        JButton btnOpen = createXPButton("Otwórz plik...");
        JButton btnSave = createXPButton("Zapisz");
        JButton btnClear = createXPButton("Wyczyść tekst");

        toolBar.add(btnOpen);
        toolBar.addSeparator();
        toolBar.add(btnSave);
        toolBar.addSeparator();
        toolBar.add(btnClear);
        add(toolBar, BorderLayout.NORTH);

        // --- POLE TEKSTOWE ---
        textArea = new JTextArea();
        textArea.setFont(xpTextFont);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        // TERAZ TEKST MOŻNA EDYTOWAĆ
        textArea.setEditable(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        JPanel textContainer = new JPanel(new BorderLayout());
        textContainer.setBorder(new EmptyBorder(5, 5, 5, 5));
        textContainer.setBackground(xpBackground);
        textContainer.add(scrollPane, BorderLayout.CENTER);
        add(textContainer, BorderLayout.CENTER);

        // --- PASEK STATUSU ---
        statusLabel = new JLabel(" Gotowy. (Możesz upuścić tu plik lub użyć przycisku Otwórz)");
        statusLabel.setFont(xpUiFont);
        statusLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        add(statusLabel, BorderLayout.SOUTH);

        // --- AKCJE PRZYCISKÓW ---
        btnOpen.addActionListener(e -> openFileWithExplorer());
        btnSave.addActionListener(e -> saveFile());
        btnClear.addActionListener(e -> clearText());

        // --- OBSŁUGA DRAG & DROP ---
        setupDragAndDrop();
    }

    // Metoda pomocnicza do tworzenia przycisków wyglądających klasycznie
    private JButton createXPButton(String text) {
        JButton button = new JButton(text);
        button.setFont(xpUiFont);
        button.setBackground(xpBackground);
        button.setFocusPainted(false); // Usuwa obwódkę po kliknięciu
        return button;
    }

    // 1. Opcja dodania pliku: przez Eksplorator Windows
    private void openFileWithExplorer() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Otwórz plik tekstowy");

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            loadFileContent(fileToOpen);
        }
    }

    // Opcja zapisu pliku
    private void saveFile() {
        if (currentFile == null) {
            // Jeśli tworzymy nowy plik, którego jeszcze nigdzie nie zapisaliśmy
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Zapisz plik jako...");
            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
            } else {
                return; // Użytkownik anulował zapisywanie
            }
        }

        try {
            // Zapisujemy tekst z okna używając tego samego kodowania znaków
            Files.writeString(currentFile.toPath(), textArea.getText(), currentCharset);
            statusLabel.setText(" Pomyślnie zapisano plik: " + currentFile.getName());
            JOptionPane.showMessageDialog(this, "Plik został zapisany!", "Zapis", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Błąd zapisu pliku:\n" + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Opcja czyszczenia tekstu
    private void clearText() {
        textArea.setText("");
        currentFile = null; // Zapominamy o starym pliku, żeby przy zapisie spytać o nową ścieżkę
        currentCharset = StandardCharsets.UTF_8;
        setTitle("Notatnik (Styl Windows XP) - Nowy plik");
        statusLabel.setText(" Wyczyszczono ekran. Gotowy do wpisywania.");
    }

    // 2. Opcja dodania pliku: Przeciągnij i upuść
    private void setupDragAndDrop() {
        new DropTarget(textArea, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            loadFileContent(files.get(0));
                        }
                    }
                } catch (Exception e) {
                    statusLabel.setText(" Błąd podczas upuszczania pliku.");
                }
            }
        });
    }

    // Logika odczytu pliku (wraz ze wsparciem dla polskich znaków)
    private void loadFileContent(File file) {
        try {
            String content;
            try {
                // Próba natywnego odczytu w UTF-8
                content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                currentCharset = StandardCharsets.UTF_8;
            } catch (IOException e) {
                // Starsze pliki z Windowsa (kodowanie Windows-1250)
                content = Files.readString(file.toPath(), Charset.forName("Windows-1250"));
                currentCharset = Charset.forName("Windows-1250");
            }

            textArea.setText(content);
            textArea.setCaretPosition(0);

            // Aktualizacja informacji po udanym wczytaniu
            currentFile = file;
            setTitle("Notatnik (Styl Windows XP) - " + file.getName());
            statusLabel.setText(" Otwarto plik: " + file.getAbsolutePath());

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Nie udało się odczytać pliku:\n" + e.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Próba załadowania klasycznego wyglądu Windows, aby spotęgować efekt retro
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
            } catch (Exception e) {
                // Jeśli niedostępne na tym systemie, system automatycznie użyje domyślnego
            }

            WinXPNotepad app = new WinXPNotepad();
            app.setVisible(true);
        });
    }
}