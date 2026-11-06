import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileSystemView;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
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
    private BufferedImage currentMediaImage;
    private double mediaZoom = 1.0;

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
        MouseWheelListener mediaZoomListener = event -> {
            if (event.isControlDown()) {
                zoomMedia(event.getWheelRotation() < 0 ? 1.1 : 1 / 1.1);
                event.consume();
            }
        };
        mediaScrollPane.addMouseWheelListener(mediaZoomListener);
        mediaScrollPane.getViewport().addMouseWheelListener(mediaZoomListener);
        mediaLabel.addMouseWheelListener(mediaZoomListener);
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
        installKeyboardShortcuts();

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

    private void installKeyboardShortcuts() {
        int control = InputEvent.CTRL_DOWN_MASK;
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, control), "open-file",
                this::openFileWithExplorer);
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_S, control), "save-file", this::saveFile);
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_N, control), "new-file", this::clearText);
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, control | InputEvent.SHIFT_DOWN_MASK),
                "increase-font", () -> changeFontSize(1));
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, control), "increase-font-plus",
                () -> changeFontSize(1));
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, control), "increase-font-numpad",
                () -> changeFontSize(1));
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, control), "decrease-font",
                () -> changeFontSize(-1));
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, control), "decrease-font-numpad",
                () -> changeFontSize(-1));
        bindShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_0, control), "reset-media-zoom", () -> {
            mediaZoom = 1.0;
            updateMediaImage();
        });
    }

    private void bindShortcut(KeyStroke keyStroke, String actionName, Runnable action) {
        Action shortcutAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        };
        JComponent rootPane = getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        rootPane.getActionMap().put(actionName, shortcutAction);
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, actionName);
        textArea.getActionMap().put(actionName, shortcutAction);
    }

    // 1. Opcja dodania pliku: przez Eksplorator Windows
    private void openFileWithExplorer() {
        File fileToOpen = showWindows8FileDialog();
        if (fileToOpen != null) {
            loadFileContent(fileToOpen);
        }
    }

    private File showWindows8FileDialog() {
        JDialog dialog = new JDialog(this, "Otwórz plik", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        File[] initialRoots = File.listRoots();
        File initialDirectory = initialRoots.length == 0 ? new File(System.getProperty("user.home")) :
                Arrays.stream(initialRoots).filter(File::isDirectory).findFirst().orElse(initialRoots[0]);
        File[] currentDirectory = {initialDirectory};
        File[] selectedFile = {null};
        List<File> backHistory = new ArrayList<>();
        boolean[] showingDriveList = {true};
        boolean[] refreshingSidebar = {false};

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(0, 16, 14, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        JLabel title = new JLabel("  Otwórz plik");
        title.setFont(new Font("Segoe UI Light", Font.PLAIN, 22));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(14, 8, 14, 8));
        header.add(title, BorderLayout.CENTER);

        JPanel locationBar = new JPanel(new BorderLayout(8, 0));
        locationBar.setBackground(BACKGROUND);
        JButton backButton = createFlatButton("←", new Color(90, 110, 125));
        backButton.setToolTipText("Wróć do poprzedniego folderu");
        backButton.setEnabled(false);
        JButton upButton = createFlatButton("↑", ACCENT);
        upButton.setToolTipText("Przejdź do folderu nadrzędnego");
        JTextField locationField = new JTextField();
        locationField.setFont(UI_FONT);
        locationField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(7, 9, 7, 9)));
        JPanel navigationButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        navigationButtons.setBackground(BACKGROUND);
        navigationButtons.add(backButton);
        navigationButtons.add(upButton);
        locationBar.add(navigationButtons, BorderLayout.WEST);
        locationBar.add(locationField, BorderLayout.CENTER);

        DefaultListModel<File> fileListModel = new DefaultListModel<>();
        JList<File> fileList = new JList<>(fileListModel);
        fileList.setFont(UI_FONT);
        fileList.setBackground(Color.WHITE);
        fileList.setForeground(TEXT_COLOR);
        fileList.setSelectionBackground(new Color(204, 232, 255));
        fileList.setSelectionForeground(TEXT_COLOR);
        fileList.setFixedCellHeight(34);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                File file = (File) value;
                setText(file.getName().isEmpty() ? file.getPath() : file.getName());
                setIcon(fileSystemView.getSystemIcon(file));
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        JTextField fileNameField = new JTextField();
        fileNameField.setFont(UI_FONT);
        fileNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(7, 9, 7, 9)));
        Runnable[] refreshSidebar = {() -> { }};

        Runnable refreshFiles = () -> {
            fileListModel.clear();
            File[] files = currentDirectory[0].listFiles(file ->
                    file.isDirectory() || isSupportedOpenFile(file));
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::isFile)
                        .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
                for (File file : files) {
                    fileListModel.addElement(file);
                }
            }
            locationField.setText(currentDirectory[0].getAbsolutePath());
            fileList.clearSelection();
        };

        Consumer<File> navigateToDirectory = target -> {
            if (target == null || !target.isDirectory()) {
                return;
            }
            if (!sameDirectory(currentDirectory[0], target)) {
                backHistory.add(currentDirectory[0]);
                currentDirectory[0] = target;
            }
            showingDriveList[0] = false;
            fileNameField.setText("");
            refreshFiles.run();
            refreshSidebar[0].run();
            backButton.setEnabled(!backHistory.isEmpty());
        };

        Runnable navigateToLocation = () -> {
            File target = new File(locationField.getText().trim());
            if (target.isDirectory()) {
                navigateToDirectory.accept(target);
            } else if (target.isFile()) {
                selectedFile[0] = target;
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Nie znaleziono folderu:\n" + target,
                        "Nieprawidłowa lokalizacja", JOptionPane.WARNING_MESSAGE);
            }
        };

        backButton.addActionListener(e -> {
            if (!backHistory.isEmpty()) {
                currentDirectory[0] = backHistory.remove(backHistory.size() - 1);
                showingDriveList[0] = false;
                fileNameField.setText("");
                refreshFiles.run();
                refreshSidebar[0].run();
                backButton.setEnabled(!backHistory.isEmpty());
            }
        });
        upButton.addActionListener(e -> {
            File parent = currentDirectory[0].getParentFile();
            if (parent != null) {
                navigateToDirectory.accept(parent);
            }
        });
        locationField.addActionListener(e -> navigateToLocation.run());
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                File selected = fileList.getSelectedValue();
                if (selected != null && selected.isFile()) {
                    fileNameField.setText(selected.getName());
                }
            }
        });
        fileList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    File selected = fileList.getSelectedValue();
                    if (selected != null && selected.isDirectory()) {
                        navigateToDirectory.accept(selected);
                    } else if (selected != null) {
                        selectedFile[0] = selected;
                        dialog.dispose();
                    }
                }
            }
        });

        JButton openButton = createFlatButton("Otwórz", ACCENT);
        JButton cancelButton = createFlatButton("Anuluj", new Color(90, 110, 125));
        openButton.addActionListener(e -> {
            String enteredName = fileNameField.getText().trim();
            File candidate = enteredName.isEmpty() ? fileList.getSelectedValue() :
                    new File(currentDirectory[0], enteredName);
            if (candidate == null) {
                return;
            }
            if (candidate.isDirectory()) {
                navigateToDirectory.accept(candidate);
            } else if (candidate.isFile() && isSupportedOpenFile(candidate)) {
                selectedFile[0] = candidate;
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Wybierz obsługiwany plik tekstowy, obraz lub PDF.",
                        "Nie można otworzyć pliku", JOptionPane.WARNING_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        fileNameField.addActionListener(e -> openButton.doClick());
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-file-dialog");
        dialog.getRootPane().getActionMap().put("close-file-dialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
            }
        });
        dialog.getRootPane().setDefaultButton(openButton);

        JPanel fileDetails = new JPanel(new BorderLayout(10, 0));
        fileDetails.setBackground(BACKGROUND);
        fileDetails.add(new JLabel("Nazwa pliku:"), BorderLayout.WEST);
        fileDetails.add(fileNameField, BorderLayout.CENTER);
        fileDetails.add(new JLabel("Tekst, obrazy i PDF"), BorderLayout.EAST);

        JPanel dialogButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dialogButtons.setBackground(BACKGROUND);
        dialogButtons.add(openButton);
        dialogButtons.add(cancelButton);
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setBackground(BACKGROUND);
        bottomPanel.add(fileDetails, BorderLayout.CENTER);
        bottomPanel.add(dialogButtons, BorderLayout.SOUTH);

        JPanel browser = new JPanel(new BorderLayout(0, 8));
        browser.setBackground(BACKGROUND);
        browser.add(locationBar, BorderLayout.NORTH);
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        fileScrollPane.getViewport().setBackground(Color.WHITE);

        DefaultListModel<File> driveListModel = new DefaultListModel<>();
        JList<File> driveList = new JList<>(driveListModel);
        driveList.setFont(UI_FONT);
        driveList.setBackground(Color.WHITE);
        driveList.setSelectionBackground(new Color(204, 232, 255));
        driveList.setFixedCellHeight(64);
        driveList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                File location = (File) value;
                setText("<html><div style='text-align:center'>" + fileSystemView.getSystemDisplayName(location)
                        + "</div></html>");
                setIcon(fileSystemView.getSystemIcon(location));
                setHorizontalAlignment(SwingConstants.CENTER);
                setVerticalAlignment(SwingConstants.CENTER);
                setHorizontalTextPosition(SwingConstants.CENTER);
                setVerticalTextPosition(SwingConstants.BOTTOM);
                setBorder(new EmptyBorder(5, 3, 5, 3));
                return this;
            }
        });
        JPanel drivePanel = new JPanel(new BorderLayout(0, 6));
        drivePanel.setBackground(BACKGROUND);
        drivePanel.setBorder(new EmptyBorder(0, 0, 0, 8));
        JLabel drivesHeading = new JLabel("DYSKI");
        drivesHeading.setFont(new Font("Segoe UI", Font.BOLD, 12));
        drivesHeading.setForeground(TOOLBAR_BACKGROUND);
        drivesHeading.setBorder(new EmptyBorder(2, 4, 2, 4));
        JButton showDrivesButton = createFlatButton("Dyski", ACCENT);
        showDrivesButton.addActionListener(e -> {
            showingDriveList[0] = true;
            refreshSidebar[0].run();
        });
        JPanel sidebarHeader = new JPanel(new BorderLayout());
        sidebarHeader.setBackground(BACKGROUND);
        sidebarHeader.add(drivesHeading, BorderLayout.CENTER);
        sidebarHeader.add(showDrivesButton, BorderLayout.EAST);
        drivePanel.add(sidebarHeader, BorderLayout.NORTH);
        JScrollPane driveScrollPane = new JScrollPane(driveList);
        driveScrollPane.setPreferredSize(new Dimension(112, 0));
        driveScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        driveScrollPane.getViewport().setBackground(Color.WHITE);
        drivePanel.add(driveScrollPane, BorderLayout.CENTER);

        refreshSidebar[0] = () -> {
            refreshingSidebar[0] = true;
            driveListModel.clear();
            if (showingDriveList[0]) {
                drivesHeading.setText("DYSKI");
                showDrivesButton.setVisible(false);
                for (File root : File.listRoots()) {
                    driveListModel.addElement(root);
                }
            } else {
                drivesHeading.setText("SZYBKIE FOLDERY");
                showDrivesButton.setVisible(true);
                File driveRoot = findDriveRoot(currentDirectory[0]);
                File userHome = new File(System.getProperty("user.home"));
                if (driveRoot != null && sameDirectory(findDriveRoot(userHome), driveRoot)) {
                    for (String folderName : List.of(
                            "Desktop", "Documents", "Downloads", "Pictures", "Music", "Videos")) {
                        File folder = new File(userHome, folderName);
                        if (folder.isDirectory()) {
                            driveListModel.addElement(folder);
                        }
                    }
                }
                if (driveListModel.isEmpty() && driveRoot != null) {
                    File[] folders = driveRoot.listFiles(File::isDirectory);
                    if (folders != null) {
                        Arrays.sort(folders, Comparator.comparing(File::getName,
                                String.CASE_INSENSITIVE_ORDER));
                        for (File folder : folders) {
                            driveListModel.addElement(folder);
                        }
                    }
                }
            }
            refreshingSidebar[0] = false;
        };
        driveList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refreshingSidebar[0]) {
                navigateToDirectory.accept(driveList.getSelectedValue());
            }
        });
        refreshSidebar[0].run();

        JPanel browserContent = new JPanel(new BorderLayout(10, 0));
        browserContent.setBackground(BACKGROUND);
        browserContent.add(drivePanel, BorderLayout.WEST);
        browserContent.add(fileScrollPane, BorderLayout.CENTER);
        browser.add(browserContent, BorderLayout.CENTER);
        browser.add(bottomPanel, BorderLayout.SOUTH);

        content.add(browser, BorderLayout.CENTER);
        dialog.setContentPane(new JPanel(new BorderLayout()));
        dialog.getContentPane().add(header, BorderLayout.NORTH);
        dialog.getContentPane().add(content, BorderLayout.CENTER);
        refreshFiles.run();
        dialog.setSize(860, 560);
        dialog.setMinimumSize(new Dimension(700, 440));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return selectedFile[0];
    }

    private boolean isSupportedOpenFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".text") || name.endsWith(".log")
                || isImageFile(file) || name.endsWith(".pdf");
    }

    private static File findDriveRoot(File location) {
        if (location == null) {
            return null;
        }
        for (File root : File.listRoots()) {
            if (location.toPath().toAbsolutePath().normalize()
                    .startsWith(root.toPath().toAbsolutePath().normalize())) {
                return root;
            }
        }
        return null;
    }

    private static boolean sameDirectory(File first, File second) {
        return first != null && second != null
                && first.toPath().toAbsolutePath().normalize()
                .equals(second.toPath().toAbsolutePath().normalize());
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
        currentMediaImage = null;
        mediaZoom = 1.0;
        mediaLabel.setIcon(null);
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

    private void zoomMedia(double factor) {
        if (currentMediaImage == null) {
            return;
        }
        mediaZoom = Math.max(0.1, Math.min(5.0, mediaZoom * factor));
        updateMediaImage();
        statusLabel.setText(" Powiększenie obrazu: " + Math.round(mediaZoom * 100) + "% (Ctrl+0: reset)");
    }

    private void updateMediaImage() {
        if (currentMediaImage == null) {
            return;
        }
        int width = Math.max(1, (int) Math.round(currentMediaImage.getWidth() * mediaZoom));
        int height = Math.max(1, (int) Math.round(currentMediaImage.getHeight() * mediaZoom));
        Image displayImage = mediaZoom == 1.0 ? currentMediaImage :
                currentMediaImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        mediaLabel.setIcon(new ImageIcon(displayImage));
        mediaLabel.revalidate();
        mediaLabel.repaint();
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
            currentMediaImage = image;
            mediaZoom = 1.0;
            updateMediaImage();
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
            mediaZoom = 1.0;
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
            currentMediaImage = pageImage;
            updateMediaImage();
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