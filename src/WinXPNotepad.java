import javax.swing.*;
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

public class ModernTextViewer extends JFrame {

    private final JTextArea textArea;
    private final JLabel statusLabel;
    private final JPanel textPanel;

    // Kolory interfejsu (jasny, przyjemny motyw)
    private final Color defaultBg = new Color(250, 250, 250);
    private final Color dragOverBg = new Color(230, 245, 255); // Jasnoniebieski po najechaniu plikiem
    private final Color borderColor = new Color(200, 200, 200);
    private final Color dragBorderColor = new Color(0, 120, 215); // Niebieska ramka

    public ModernTextViewer() {
        setTitle("📄 Przyjemna Przeglądarka Plików");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Konfiguracja głównego pola tekstowego
        textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setText("\n\n\n\n\n\t\tZłap plik tekstowy (.txt) i upuść go tutaj...");
        textArea.setForeground(new Color(100, 100, 100)); // Szary tekst podpowiedzi

        // Panel owijający pole tekstowe (dla ładnych marginesów)
        textPanel = new JPanel(new BorderLayout());
        textPanel.setBackground(defaultBg);
        textPanel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(borderColor, 2, true)
        ));

        textArea.setBackground(defaultBg);
        textArea.setBorder(new EmptyBorder(15, 15, 15, 15)); // Wewnętrzny margines dla tekstu

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null); // Usunięcie domyślnej, brzydkiej ramki paska przewijania
        textPanel.add(scrollPane, BorderLayout.CENTER);

        // Kolorowy pasek statusu na dole
        statusLabel = new JLabel(" Oczekuję na plik...", SwingConstants.LEFT);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(0, 120, 215)); // Głęboki niebieski
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        add(textPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Inicjalizacja naprawionego Drag & Drop
        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        // DropTarget to znacznie lepsze narzędzie do obsługi plików w Swing niż TransferHandler
        new DropTarget(textArea, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                // Zmiana wyglądu po najechaniu plikiem (wizualny feedback)
                textArea.setBackground(dragOverBg);
                textPanel.setBackground(dragOverBg);
                textPanel.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(10, 10, 10, 10),
                        BorderFactory.createLineBorder(dragBorderColor, 2, true)
                ));
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                // Powrót do domyślnego wyglądu, gdy plik opuści okno
                resetVisuals();
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                resetVisuals(); // Przywrócenie kolorów
                try {
                    // Akceptacja upuszczonego elementu
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                        if (!files.isEmpty()) {
                            loadFileContent(files.get(0)); // Odczytanie pierwszego pliku
                        }
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ModernTextViewer.this,
                            "Błąd podczas upuszczania pliku: " + e.getMessage(),
                            "Błąd", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void resetVisuals() {
        textArea.setBackground(defaultBg);
        textPanel.setBackground(defaultBg);
        textPanel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(borderColor, 2, true)
        ));
    }

    private void loadFileContent(File file) {
        try {
            String content;
            try {
                // Pierwsze podejście: nowszy standard UTF-8 (obsługuje polskie znaki)
                content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                // Drugie podejście: starsze pliki z notatnika Windows (Windows-1250)
                content = Files.readString(file.toPath(), Charset.forName("Windows-1250"));
            }

            textArea.setForeground(new Color(30, 30, 30)); // Zmiana koloru tekstu na ciemny
            textArea.setText(content);
            textArea.setCaretPosition(0); // Przewinięcie tekstu na samą górę
            statusLabel.setText(" Otwarto plik: " + file.getAbsolutePath());
            setTitle("📄 " + file.getName() + " - Przeglądarka");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Nie udało się odczytać pliku. Upewnij się, że to plik tekstowy.\nSzczegóły: " + e.getMessage(),
                    "Błąd odczytu", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Włączenie nowoczesnego wyglądu przycisków i pasków dla danego systemu (Windows/Mac)
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            ModernTextViewer viewer = new ModernTextViewer();
            viewer.setVisible(true);
        });
    }
}