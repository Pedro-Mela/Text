import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class WinXPNotepad extends JFrame {

    private final JTextArea textArea;
    private final JLabel statusLabel;
    private final JLabel mediaLabel;
    private final JLabel pdfPageLabel;
    private final JButton previousPageButton;
    private final JButton nextPageButton;
    private final JButton saveButton;
    private final JComboBox<String> fontSelector;
    private final CardLayout contentCardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(contentCardLayout);

    // Zmienne do przechowywania informacji o aktualnie otwartym pliku
    private File currentFile = null;
    private Charset currentCharset = StandardCharsets.UTF_8;
    private PDDocument currentPdf;
    private int currentPdfPage;
    private int currentFontSize = 16;
    private String currentFontFamily = "Segoe UI";

    private static final Color BACKGROUND = new Color(235, 243, 248);
    private static final Color ACCENT = new Color(0, 120, 215);
    private static final Color TOOLBAR_BACKGROUND = new Color(0, 90, 158);
    private static final Color TEXT_COLOR = new Color(45, 50, 56);
    private static final Color BORDER_COLOR = new Color(220, 224, 228);
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    public WinXPNotepad() {
        setTitle("Notatnik - Nowy plik");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ACCENT);

        JLabel heading = new JLabel("NOTATNIK");
        heading.setFont(new Font("Segoe UI Light", Font.PLAIN, 28));
        heading.setForeground(Color.WHITE);
        heading.setBorder(new EmptyBorder(18, 22, 16, 22));
        topPanel.add(heading, BorderLayout.CENTER);

        JPanel commandBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 6));
        commandBar.setBackground(TOOLBAR_BACKGROUND);
        commandBar.setBorder(new EmptyBorder(2, 17, 8, 17));

        JButton btnOpen = createFlatButton("Otwórz plik", new Color(0, 164, 239));
        saveButton = createFlatButton("Zapisz", new Color(0, 163, 0));
        JButton btnClear = createFlatButton("Wyczyść", new Color(227, 162, 26));
        commandBar.add(btnOpen);
        commandBar.add(saveButton);
        commandBar.add(btnClear);
        commandBar.add(Box.createHorizontalStrut(12));
        commandBar.add(new JLabel("Czcionka:"));
        fontSelector = new JComboBox<>(Arrays.stream(
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new));
        fontSelector.setPreferredSize(new Dimension(165, 34));
        fontSelector.setSelectedItem(currentFontFamily);
        commandBar.add(fontSelector);
        JButton decreaseFontButton = createFlatButton("A−", new Color(90, 110, 125));
        JButton increaseFontButton = createFlatButton("A+", new Color(90, 110, 125));
        commandBar.add(decreaseFontButton);
        commandBar.add(increaseFontButton);
        topPanel.add(commandBar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        textArea = new JTextArea();
        updateTextFont();
        textArea.setForeground(TEXT_COLOR);
        textArea.setBackground(Color.WHITE);
        textArea.setCaretColor(ACCENT);
        textArea.setMargin(new Insets(16, 18, 16, 18));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setBackground(BACKGROUND);
        scrollPane.getHorizontalScrollBar().setBackground(BACKGROUND);

        JPanel textContainer = new JPanel(new BorderLayout());
        textContainer.setBorder(new EmptyBorder(16, 20, 14, 20));
        textContainer.setBackground(BACKGROUND);
        textContainer.add(scrollPane, BorderLayout.CENTER);
        contentCards.add(textContainer, "text");

        JPanel mediaPanel = new JPanel(new BorderLayout(0, 8));
        mediaPanel.setBackground(BACKGROUND);
        mediaPanel.setBorder(new EmptyBorder(16, 20, 14, 20));
        mediaLabel = new JLabel();
        mediaLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mediaLabel.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane mediaScrollPane = new JScrollPane(mediaLabel);
        mediaScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        mediaScrollPane.getViewport().setBackground(Color.WHITE);
        mediaPanel.add(mediaScrollPane, BorderLayout.CENTER);

        JPanel pdfNavigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pdfNavigation.setBackground(BACKGROUND);
        previousPageButton = createFlatButton("‹ Poprzednia", ACCENT);
        nextPageButton = createFlatButton("Następna ›", ACCENT);
        pdfPageLabel = new JLabel(" ");
        pdfPageLabel.setFont(UI_FONT);
        pdfNavigation.add(previousPageButton);
        pdfNavigation.add(pdfPageLabel);
        pdfNavigation.add(nextPageButton);
        mediaPanel.add(pdfNavigation, BorderLayout.SOUTH);
        contentCards.add(mediaPanel, "media");
        add(contentCards, BorderLayout.CENTER);

        statusLabel = new JLabel(" Gotowy. (Możesz upuścić tu plik lub użyć przycisku Otwórz)");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(3, 0, 0, 0, new Color(0, 164, 239)),
                new EmptyBorder(9, 20, 9, 20)));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(TOOLBAR_BACKGROUND);
        add(statusLabel, BorderLayout.SOUTH);

        btnOpen.addActionListener(e -> openFileWithExplorer());
        saveButton.addActionListener(e -> saveFile());
        btnClear.addActionListener(e -> clearText());
        fontSelector.addActionListener(e -> {
            Object selectedFont = fontSelector.getSelectedItem();
            if (selectedFont != null) {
                currentFontFamily = selectedFont.toString();
                updateTextFont();
            }
        });
        decreaseFontButton.addActionListener(e -> changeFontSize(-1));
        increaseFontButton.addActionListener(e -> changeFontSize(1));
        previousPageButton.addActionListener(e -> showPdfPage(currentPdfPage - 1));
        nextPageButton.addActionListener(e -> showPdfPage(currentPdfPage + 1));
        updatePdfNavigation();

        setupDragAndDrop();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeCurrentPdf();
            }
        });
    }

    private JButton createFlatButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(UI_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setBorder(new EmptyBorder(11, 18, 11, 18));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setRolloverEnabled(true);
        button.addChangeListener(e -> {
            ButtonModel model = button.getModel();
            if (model.isPressed() || model.isRollover()) {
                button.setBackground(color.brighter());
            } else {
                button.setBackground(color);
            }
        });
        return button;
    }

    // 1. Opcja dodania pliku: przez Eksplorator Windows
    private void openFileWithExplorer() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Otwórz plik");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Tekst, obrazy i PDF", "txt", "text", "log", "png", "jpg", "jpeg", "gif", "bmp",
                "tif", "tiff", "pdf"));

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            loadFileContent(fileToOpen);
        }
    }

    // Opcja zapisu pliku
    private void saveFile() {
        if (currentPdf != null || isImageFile(currentFile)) {
            return;
        }
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
        closeCurrentPdf();
        textArea.setText("");
        currentFile = null; // Zapominamy o starym pliku, żeby przy zapisie spytać o nową ścieżkę
        currentCharset = StandardCharsets.UTF_8;
        setTitle("Notatnik - Nowy plik");
        statusLabel.setText(" Wyczyszczono ekran. Gotowy do wpisywania.");
        saveButton.setEnabled(true);
        fontSelector.setEnabled(true);
        contentCardLayout.show(contentCards, "text");
        updatePdfNavigation();
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
        if (file.getName().toLowerCase().endsWith(".pdf")) {
            loadPdf(file);
            return;
        }
        if (isImageFile(file)) {
            loadImage(file);
            return;
        }

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
            closeCurrentPdf();
            saveButton.setEnabled(true);
            fontSelector.setEnabled(true);
            contentCardLayout.show(contentCards, "text");
            setTitle("Notatnik - " + file.getName());
            statusLabel.setText(" Otwarto plik: " + file.getAbsolutePath());

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Nie udało się odczytać pliku:\n" + e.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTextFont() {
        textArea.setFont(new Font(currentFontFamily, Font.PLAIN, currentFontSize));
    }

    private void changeFontSize(int change) {
        currentFontSize = Math.max(8, Math.min(72, currentFontSize + change));
        updateTextFont();
        statusLabel.setText(" Rozmiar czcionki: " + currentFontSize);
    }

    private boolean isImageFile(File file) {
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".tif")
                || name.endsWith(".tiff");
    }

    private void loadImage(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Nieobsługiwany lub uszkodzony plik obrazu.");
            }
            closeCurrentPdf();
            mediaLabel.setIcon(new ImageIcon(image));
            pdfPageLabel.setText("Obraz: " + image.getWidth() + " × " + image.getHeight());
            saveButton.setEnabled(false);
            fontSelector.setEnabled(false);
            previousPageButton.setVisible(false);
            nextPageButton.setVisible(false);
            contentCardLayout.show(contentCards, "media");
            currentFile = file;
            setTitle("Podgląd obrazu - " + file.getName());
            statusLabel.setText(" Wyświetlono obraz: " + file.getName());
        } catch (IOException e) {
            showFileError("Nie udało się wyświetlić obrazu", e);
        }
    }

    private void loadPdf(File file) {
        try {
            PDDocument document = PDDocument.load(file);
            if (document.getNumberOfPages() == 0) {
                document.close();
                throw new IOException("Plik PDF nie zawiera stron.");
            }
            closeCurrentPdf();
            currentPdf = document;
            currentPdfPage = 0;
            saveButton.setEnabled(false);
            fontSelector.setEnabled(false);
            mediaLabel.setIcon(null);
            previousPageButton.setVisible(true);
            nextPageButton.setVisible(true);
            contentCardLayout.show(contentCards, "media");
            currentFile = file;
            setTitle("Podgląd PDF - " + file.getName());
            showPdfPage(0);
        } catch (IOException e) {
            showFileError("Nie udało się otworzyć pliku PDF", e);
        }
    }

    private void showPdfPage(int pageIndex) {
        if (currentPdf == null || pageIndex < 0 || pageIndex >= currentPdf.getNumberOfPages()) {
            return;
        }
        try {
            BufferedImage pageImage = new PDFRenderer(currentPdf).renderImageWithDPI(pageIndex, 110);
            currentPdfPage = pageIndex;
            mediaLabel.setIcon(new ImageIcon(pageImage));
            pdfPageLabel.setText("Strona " + (pageIndex + 1) + " z " + currentPdf.getNumberOfPages());
            updatePdfNavigation();
            statusLabel.setText(" Wyświetlono stronę " + (pageIndex + 1) + " z " + currentFile.getName());
        } catch (IOException e) {
            showFileError("Nie udało się wyświetlić strony PDF", e);
        }
    }

    private void updatePdfNavigation() {
        boolean showingPdf = currentPdf != null;
        previousPageButton.setEnabled(showingPdf && currentPdfPage > 0);
        nextPageButton.setEnabled(showingPdf && currentPdfPage < currentPdf.getNumberOfPages() - 1);
        if (!showingPdf) {
            pdfPageLabel.setText(" ");
        }
    }

    private void closeCurrentPdf() {
        if (currentPdf != null) {
            try {
                currentPdf.close();
            } catch (IOException e) {
                showFileError("Nie udało się zamknąć pliku PDF", e);
            } finally {
                currentPdf = null;
                currentPdfPage = 0;
            }
        }
    }

    private void showFileError(String message, IOException error) {
        JOptionPane.showMessageDialog(this, message + ":\n" + error.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Nie udało się załadować systemowego wyglądu: " + e.getMessage());
            }

            WinXPNotepad app = new WinXPNotepad();
            app.setVisible(true);
        });
    }
}