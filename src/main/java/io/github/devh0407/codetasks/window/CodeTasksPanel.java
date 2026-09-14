package io.github.devh0407.codetasks.window;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.devh0407.codetasks.model.ExternalIssue;
import io.github.devh0407.codetasks.model.IssueType;
import io.github.devh0407.codetasks.service.CodeTasksSettings;
import io.github.devh0407.codetasks.service.ExternalIssueService;
import io.github.devh0407.codetasks.service.FileResolver;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CodeTasksPanel extends JPanel {
    private final Project project;
    private final ExternalIssueService issueService;
    private final FileResolver fileResolver;

    private final IssueTableModel tableModel = new IssueTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<IssueTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JTextField sourceField = new JTextField();
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"All", "TODO", "PROBLEM"});
    private final JLabel statusLabel = new JLabel("No source selected");

    public CodeTasksPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.issueService = ExternalIssueService.getInstance(project);
        this.fileResolver = new FileResolver(project);

        buildUi();
        bindEvents();

        sourceField.setText(CodeTasksSettings.getInstance(project).getSourcePath());
        refreshFromService();

        issueService.addChangeListener(this::refreshFromService);
        if (!sourceField.getText().isBlank() && issueService.getIssues().isEmpty()) {
            issueService.reloadAsync();
        }
    }

    private void buildUi() {
        JPanel sourcePanel = new JPanel(new BorderLayout(6, 0));
        sourcePanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 3, 6));
        sourcePanel.add(sourceField, BorderLayout.CENTER);

        JPanel sourceButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton chooseButton = new JButton("Choose File");
        chooseButton.setActionCommand("choose");
        JButton reloadButton = new JButton("Reload");
        reloadButton.setActionCommand("reload");
        sourceButtons.add(chooseButton);
        sourceButtons.add(reloadButton);
        sourcePanel.add(sourceButtons, BorderLayout.EAST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Type:"));
        filterPanel.add(typeFilter);
        filterPanel.add(statusLabel);

        JPanel north = new JPanel(new BorderLayout());
        north.add(sourcePanel, BorderLayout.NORTH);
        north.add(filterPanel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        add(new JScrollPane(table), BorderLayout.CENTER);

        chooseButton.addActionListener(event -> chooseSourceFile());
        reloadButton.addActionListener(event -> reloadSource());
    }

    private void bindEvents() {
        DocumentListener filterListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        };
        searchField.getDocument().addDocumentListener(filterListener);
        typeFilter.addActionListener(event -> applyFilter());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    navigateToSelectedIssue();
                }
            }
        });
    }

    private void chooseSourceFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Code Tasks CSV/XLSX");
        chooser.setFileFilter(new FileNameExtensionFilter("Code Tasks (*.csv, *.xls, *.xlsx)", "csv", "xls", "xlsx"));

        String current = sourceField.getText().trim();
        if (!current.isBlank()) {
            File currentFile = new File(current);
            File parent = currentFile.getParentFile();
            if (parent != null && parent.isDirectory()) {
                chooser.setCurrentDirectory(parent);
            }
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String selected = chooser.getSelectedFile().getAbsolutePath();
            sourceField.setText(selected);
            issueService.setSourcePath(selected);
        }
    }

    private void reloadSource() {
        String path = sourceField.getText().trim();
        if (path.isBlank()) {
            CodeTasksSettings.getInstance(project).setSourcePath("");
            issueService.setSourcePath("");
            return;
        }
        issueService.setSourcePath(path);
    }

    private void refreshFromService() {
        tableModel.setIssues(issueService.getIssues());
        applyFilter();

        if (issueService.isLoading()) {
            statusLabel.setText("Loading…");
            return;
        }
        if (!issueService.getLastError().isBlank()) {
            statusLabel.setText("Error: " + issueService.getLastError());
            return;
        }

        long todoCount = issueService.getIssues().stream().filter(i -> i.type() == IssueType.TODO).count();
        long problemCount = issueService.getIssues().stream().filter(i -> i.type() == IssueType.PROBLEM).count();
        statusLabel.setText(todoCount + " TODO · " + problemCount + " PROBLEM");
    }

    private void applyFilter() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedType = String.valueOf(typeFilter.getSelectedItem());

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends IssueTableModel, ? extends Integer> entry) {
                ExternalIssue issue = tableModel.getIssue(entry.getIdentifier());
                if (!"All".equals(selectedType) && !issue.type().name().equals(selectedType)) {
                    return false;
                }
                if (query.isBlank()) {
                    return true;
                }
                return issue.id().toLowerCase(Locale.ROOT).contains(query)
                        || issue.filePath().toLowerCase(Locale.ROOT).contains(query)
                        || issue.message().toLowerCase(Locale.ROOT).contains(query)
                        || issue.source().toLowerCase(Locale.ROOT).contains(query);
            }
        });
    }

    private void navigateToSelectedIssue() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedViewRow);
        ExternalIssue issue = tableModel.getIssue(modelRow);
        VirtualFile file = fileResolver.resolve(issue.filePath()).orElse(null);
        if (file == null) {
            Messages.showWarningDialog(
                    project,
                    "Unable to resolve file: " + issue.filePath(),
                    "Code Tasks"
            );
            return;
        }

        new OpenFileDescriptor(project, file, issue.line() - 1, 0).navigate(true);
    }

    private static final class IssueTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Type", "File", "Line", "Message", "ID"};
        private List<ExternalIssue> issues = List.of();

        public void setIssues(List<ExternalIssue> issues) {
            this.issues = List.copyOf(new ArrayList<>(issues));
            fireTableDataChanged();
        }

        public ExternalIssue getIssue(int row) {
            return issues.get(row);
        }

        @Override
        public int getRowCount() {
            return issues.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 2 ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ExternalIssue issue = issues.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> issue.type().name();
                case 1 -> issue.filePath();
                case 2 -> issue.line();
                case 3 -> issue.message();
                case 4 -> issue.id();
                default -> "";
            };
        }
    }
}
