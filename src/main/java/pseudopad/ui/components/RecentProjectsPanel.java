package pseudopad.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import pseudopad.app.MainFrame;
import pseudopad.utils.IconManager;
import pseudopad.utils.PreferenceManager;

/**
 * Displays a list of recently opened projects.
 */
public class RecentProjectsPanel extends JPanel {

    private final JList<File> list;
    private final DefaultListModel<File> listModel;
    private final MainFrame mainFrame;

    public RecentProjectsPanel(MainFrame mainFrame) {
        super(new BorderLayout());
        this.mainFrame = mainFrame;

        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ProjectListCellRenderer());

        // Handle clicks
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        File project = listModel.getElementAt(index);
                        RecentProjectsPanel.this.mainFrame.openProject(project);
                    }
                }
            }
        });

        add(new JScrollPane(list), BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            List<File> projects = PreferenceManager.getInstance().getRecentProjects();
            for (File p : projects) {
                listModel.addElement(p);
            }
        });
    }

    private static class ProjectListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof File file) {
                setText("<html><b>" + file.getName() + "</b><br><span style='color:gray;font-size:85%'>"
                        + file.getAbsolutePath() + "</span></html>");
                setIcon(IconManager.get("project")); // Ensure project.svg exists or map to correct one
                setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
            return this;
        }
    }
}
